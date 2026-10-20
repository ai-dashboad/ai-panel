package com.aipanel.user.data.mysql.mapper;

import com.aipanel.user.data.mysql.po.UserPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * User mapper interface
 */
@Mapper
public interface UserMapper extends BaseMapper<UserPO> {

    @Select("SELECT * FROM t_user WHERE username = #{username}")
    UserPO findByUsername(@Param("username") String username);

    @Select("SELECT * FROM t_user WHERE email = #{email}")
    UserPO findByEmail(@Param("email") String email);

    @Select("SELECT * FROM t_user WHERE username = #{usernameOrEmail} OR email = #{usernameOrEmail}")
    UserPO findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);

    @Select("SELECT COUNT(1) > 0 FROM t_user WHERE username = #{username}")
    boolean existsByUsername(@Param("username") String username);

    @Select("SELECT COUNT(1) > 0 FROM t_user WHERE email = #{email}")
    boolean existsByEmail(@Param("email") String email);

    @Update("UPDATE t_user SET last_login_at = #{lastLoginAt}, updated_at = #{lastLoginAt} WHERE id = #{userId}")
    int updateLastLogin(@Param("userId") Long userId, @Param("lastLoginAt") LocalDateTime lastLoginAt);
}
