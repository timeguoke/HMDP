package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 根据id查询用户
     *
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) {
        //1.根据id查询Redis中的商铺
        String key = CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {//不为空
            //3.存在返回商铺信息

            //先将JSON转换成shop型
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            //返回
            return Result.ok(shop);

        }

        //判断命中的是否为空值
        if (shopJson != null) {
            return Result.fail("店铺信息不存在,数值为空");
        }

        //4.不存在查询数据库
        Shop shop = getById(id);
        //5.判断数据库是否存在
        if (shop == null) {
            //进行缓存空对象,防止缓存穿透
            //6.不存在,向Redis中添加空值,进行缓存空对象
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return Result.fail("店铺不存在");
        }


        //7.存在先写入Redis进行缓存,在返回商铺信息
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);

        return Result.ok(shop);
    }

    /**
     * 更新商铺
     *
     * @param shop
     * @return
     */
    @Override
    @Transactional
    public Result updateShop(Shop shop) {
        //判断id信息
        if (shop.getId() == null) {
            return Result.fail("店铺id不能为空");
        }

        //先更新数据库商铺信息
        updateById(shop);
        //删除对应的Redis缓存
        String key = CACHE_SHOP_KEY + shop.getId();
        stringRedisTemplate.delete(key);
        //不需要重新插入缓存了,当再有人查询页面会自动缓存
        return Result.ok();
    }
}
