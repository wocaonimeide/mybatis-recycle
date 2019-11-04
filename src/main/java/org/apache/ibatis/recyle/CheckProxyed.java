package org.apache.ibatis.recyle;

public interface CheckProxyed {

    /**
     * 检查当前对象是否被代理了
     * @return true 被代理了
     * @return false 没被代理
     */
    Boolean checkProxyed();

    /**
     * 更换是否代理状态
     */
    void changeProxyed(Boolean newProxyed);
}
