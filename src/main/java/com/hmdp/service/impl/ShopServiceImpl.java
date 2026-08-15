package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    @Resource
    private CacheClient cacheClient;

    /**
     * 根据id查询用户
     *
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) {
        //缓存穿透
        //Shop shop = queryWithPassThrough(id);
        Shop shop = cacheClient
                .queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);


        //互斥锁解决缓存击穿
        //Shop shop = queryWithMutex(id);
        //逻辑过期解决缓存击穿
//        Shop shop = cacheClient
//                .queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //判断shop是否为空
        if (shop == null) {
            return Result.fail("店铺不存在");
        }


        //返回
        return Result.ok(shop);
    }

    /**
     * 定义一个线程池
     */
    /*private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);*/

    /**
     * 逻辑过期解决缓存击穿
     *
     * @param id
     * @return
     */
    /*public Shop queryWithLogicalExpire(Long id) {
        //1.根据id查询Redis中的商铺
        String key = CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否存在
        if (StrUtil.isBlank(shopJson)) {
            return null;
        }
        // 4.命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        JSONObject data = (JSONObject) redisData.getData();
        //获取商铺信息
        Shop shop = JSONUtil.toBean(data, Shop.class);
        //获取过期时间
        LocalDateTime expireTime = redisData.getExpireTime();


        // 5.判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            // 5.1.未过期，直接返回店铺信息
            return shop;
        }
        // 5.2.已过期，需要缓存重建

        // 6.缓存重建

        // 6.1.获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        // 6.2.判断是否获取锁成功
        if (isLock) {
            // 6.3.成功，开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 重建缓存
                    this.saveShop2Redis(id, 20L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 释放锁
                    unLock(lockKey);
                }
            });
        }
        // 6.4.返回过期的商铺信息


        return shop;
    }*/

    /**
     * 互斥锁解决缓存击穿
     *
     * @param id
     * @return
     */
    /*public Shop queryWithMutex(Long id) {
        //1.根据id查询Redis中的商铺
        String key = CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {//不为空
            //3.存在返回商铺信息

            //先将JSON转换成shop型
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            //返回
            return shop;

        }

        //判断命中的是否为空值
        if (shopJson != null) {
            return null;
        }
        Shop shop = null;

        try {
            // 4.实现缓存重建
            // 4.1.获取互斥锁
            boolean isLock = tryLock(LOCK_SHOP_KEY + id);
            // 4.2.判断是否获取成功
            if (isLock) {
                // 4.3.失败，则休眠并重试
                Thread.sleep(50);
                return queryWithMutex(id);

            }
            // 4.4.成功，根据id查询数据库
            shop = getById(id);
            //5.判断数据库是否存在
            if (shop == null) {
                //进行缓存空对象,防止缓存穿透
                //6.不存在,向Redis中添加空值,进行缓存空对象
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }


            //7.存在先写入Redis进行缓存,在返回商铺信息
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //8.释放锁
            unLock(LOCK_SHOP_KEY + id);
        }
        //9.返回
        return shop;
    }*/

    /**
     * 缓存穿透
     * 缓存穿透的代码,不删了,用来看看穿透的解决方式
     *
     * @param id
     * @return
     */
    /*public Shop queryWithPassThrough(Long id) {
        //1.根据id查询Redis中的商铺
        String key = CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {//不为空
            //3.存在返回商铺信息

            //先将JSON转换成shop型
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            //返回
            return shop;

        }

        //判断命中的是否为空值
        if (shopJson != null) {
            return null;
        }

        //4.不存在查询数据库
        Shop shop = getById(id);
        //5.判断数据库是否存在
        if (shop == null) {
            //进行缓存空对象,防止缓存穿透
            //6.不存在,向Redis中添加空值,进行缓存空对象
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }


        //7.存在先写入Redis进行缓存,在返回商铺信息
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);

        return shop;
    }*/

    /**
     * 获取锁
     *
     * @param key
     * @return
     */
    /*private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }*/

    /**
     * 释放锁
     *
     * @param key
     * @return
     */
   /* private void unLock(String key) {
        stringRedisTemplate.delete(key);

    }*/

    /**
     * shop的逻辑封装
     *
     * @param id
     * @param expireTime
     */
    public void saveShop2Redis(Long id, Long expireTime) throws InterruptedException {
        //获取店铺数据
        Shop shop = getById(id);
        Thread.sleep(200);
        //封装过期逻辑时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireTime));
        //将数据写入Redis当中
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));


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
