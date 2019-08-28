package com.alvis.exam.configuration.spring.wx;

import com.alvis.exam.base.SystemCode;
import com.alvis.exam.configuration.spring.security.RestUtil;
import com.alvis.exam.domain.User;
import com.alvis.exam.domain.UserToken;
import com.alvis.exam.service.UserService;
import com.alvis.exam.service.UserTokenService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Component

public class TokenHandlerInterceptor implements HandlerInterceptor {
    private final static ThreadLocal<User> USER_THREAD_LOCAL = new ThreadLocal<>();
    private final UserTokenService userTokenService;
    private final UserService userService;

    @Autowired
    public TokenHandlerInterceptor(UserTokenService userTokenService, UserService userService) {
        this.userTokenService = userTokenService;
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("token");
        if (StringUtils.isEmpty(token)) {
            RestUtil.response(response, SystemCode.UNAUTHORIZED);
            return false;
        }
        UserToken userToken = userTokenService.getToken(token);
        if (null == userToken) {
            RestUtil.response(response, SystemCode.UNAUTHORIZED);
            return false;
        }

        Date now = new Date();
        User user = userService.getUserByUserName(userToken.getUserName());
        if (now.before(userToken.getEndTime())) {
            USER_THREAD_LOCAL.set(user);
            return true;
        } else {   //refresh token
            UserToken refreshToken = userTokenService.insertUserToken(user);
            RestUtil.response(response, SystemCode.AccessTokenError.getCode(), SystemCode.AccessTokenError.getMessage(), refreshToken.getToken());
            return false;
        }
    }

    public static ThreadLocal<User> getUserThreadLocal() {
        return USER_THREAD_LOCAL;
    }
}
