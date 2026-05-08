package com.erp.security.user;

import com.erp.entity.system.Employee;
import com.erp.mapper.system.EmployeeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security UserDetailsService实现
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private EmployeeMapper employeeMapper;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Employee employee = employeeMapper.selectByLoginAccount(username);
        if (employee == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // TODO: 从数据库加载用户的角色和权限
        // 这里暂时返回空的角色和权限列表，后续需要从EMPLOYEE_ROLE和ROLE_PERMISSION表查询
        UserDetailsImpl userDetails = new UserDetailsImpl(employee);
        // userDetails.setRoles(roleList);
        // userDetails.setPermissions(permissionList);

        return userDetails;
    }
}





