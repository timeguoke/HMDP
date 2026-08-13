package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;

import java.util.HashMap;
import java.util.Map;
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
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            //如果不符合返回错误信息
            return Result.fail("手机号格式错误");

        }
        //符合生成验证码
        String code = RandomUtil.randomNumbers(6);

        /*//保存验证码到session
        session.setAttribute("code", code);*/
        //保存验证码到Redis中            set: key value 时间 时间单位
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

        //发送短信成功
        log.info("短信发送成功{}", code);
        //返回ok
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1.校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            //如果不符合返回错误信息
            return Result.fail("手机号格式错误");

        }

        // 2.校验验证码
        //先从Redis取出发送的验证码
        String cachecode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);

        //再取出登录填写的验证码
        String code = loginForm.getCode();

        if (cachecode == null ||!cachecode.equals(code)) {
            //说明没有发出过验证码,或者验证码已经过期,后面的说明不一致
            // 3.不一致，报错
            return Result.fail("验证码错误");
        }



        // 4.一致，根据手机号查询用户
        //select * from tb_user where phone =?
        //query()就相当于select * from tb_user
        //eq相当于等于
        User user = query().eq("phone", phone).one();


        // 5.判断用户是否存在
        if (user == null) {
            // 6.不存在，创建新用户并保存
            user = creatUserWithPhone(phone);

        }


        /*// 7.保存用户信息到session中
        session.setAttribute("user", user);*/
        // TODO 7.保存用户信息到 redis中
        // 7.1.随机生成token，作为登录令牌
        String token = UUID.randomUUID().toString(true);
        // TODO 7.2.将User对象转为Hash存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        // TODO 7.3.存储
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY+token,userMap);
        //TODO 7.4.设置token的有效期
        stringRedisTemplate.expire(LOGIN_USER_KEY+token,LOGIN_USER_TTL,TimeUnit.MINUTES);

//        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));

        // TODO 8.返回token
        return Result.ok(token);
    }

    private User creatUserWithPhone(String phone) {
        //创建用户
        User user = new User();
        user.setCreateTime(LocalDateTime.now());
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomNumbers(6));
        //保存用户
        save(user);//这个是mybatis-plus的功能
        return null;
    }

}
