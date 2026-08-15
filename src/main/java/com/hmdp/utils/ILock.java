package com.hmdp.utils;

public interface ILock {
    /**
     * 尝试获取锁
     *
     * @param timeOutSec 锁持有的过期时间,超时锁会自动释放
     * @return true代表获取锁成功  false代表获取锁失败
     */
    boolean tryLock(long timeOutSec);

    /**
     * 释放锁
     */
    void unLock();

}
