package com.erp.security.filter;

import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.result.Result;
import com.erp.security.token.JwtTokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JWT认证过滤器
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = getTokenFromRequest(request);

        if (StringUtils.hasText(token)) {
            try {
                // 1. 从Token中获取用户名
                String username = jwtTokenUtil.getUsernameFromToken(token);
                
                if (username == null) {
                    log.warn("无法从Token中获取用户名，Token可能已过期或无效");
                    handleTokenError(response, ResultCodeEnum.TOKEN_INVALID);
                    return;
                }

                // 2. 验证Token（只依赖JWT本身的签名与过期时间）
                if (validateToken(token, username)) {
                    // 3. 检查SecurityContext中是否已有认证信息
                    if (SecurityContextHolder.getContext().getAuthentication() == null) {
                        // 4. 加载用户信息
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                        // 5. 设置认证信息
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } else {
                    log.warn("Token验证失败: {}", username);
                    handleTokenError(response, ResultCodeEnum.TOKEN_INVALID);
                    return;
                }
            } catch (ExpiredJwtException e) {
                log.warn("JWT Token已过期: {}", e.getMessage());
                handleTokenError(response, ResultCodeEnum.TOKEN_EXPIRED);
                return;
            } catch (MalformedJwtException e) {
                log.warn("JWT Token格式错误: {}", e.getMessage());
                handleTokenError(response, ResultCodeEnum.TOKEN_INVALID);
                return;
            } catch (UnsupportedJwtException e) {
                log.warn("不支持的JWT Token: {}", e.getMessage());
                handleTokenError(response, ResultCodeEnum.TOKEN_INVALID);
                return;
            } catch (Exception e) {
                log.error("JWT认证失败", e);
                handleTokenError(response, ResultCodeEnum.TOKEN_INVALID);
                return;
            }
        }

        chain.doFilter(request, response);
    }
    
    /**
     * 处理Token错误，返回JSON响应
     */
    private void handleTokenError(HttpServletResponse response, ResultCodeEnum errorCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        
        Result<?> result = Result.error(errorCode.getCode(), errorCode.getMessage());
        String json = objectMapper.writeValueAsString(result);
        response.getWriter().write(json);
        response.getWriter().flush();
    }

    /**
     * 验证Token（只做JWT本身的验证）
     */
    private boolean validateToken(String token, String username) {
        try {
            return jwtTokenUtil.validateToken(token, username);
        } catch (Exception e) {
            log.error("Token验证异常", e);
            return false;
        }
    }

    /**
     * 从请求中获取Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
















