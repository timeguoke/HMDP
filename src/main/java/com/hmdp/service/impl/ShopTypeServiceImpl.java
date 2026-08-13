package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.CACHE_SHOPTYPE_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
  @Autowired
   private ShopTypeMapper shopTypeMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 商铺类型展示
     * @return
     */
    @Override
    public Result queryTypeList() {
        //1. 查询Redis中的商铺种类
        String key = CACHE_SHOPTYPE_KEY;
        String json = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否存在
        if(StrUtil.isNotBlank(json)){
            List<ShopType> shopTypeList = JSONUtil.toList(json, ShopType.class);
            return Result.ok(shopTypeList);
        }

        //4.不存在查询数据库
        List<ShopType> shopTypesList = shopTypeMapper.selectList(null);
        //5.判断数据库是否存在
        if (shopTypesList.isEmpty()) {
            //6.不存在返回错误
            return Result.fail("店铺类型不存在");
        }


        //7.存在先写入Redis进行缓存,在返回商铺信息
        //写入
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shopTypesList));

        return Result.ok(shopTypesList);


    }
}
