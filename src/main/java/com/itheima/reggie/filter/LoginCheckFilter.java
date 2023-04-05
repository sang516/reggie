package com.itheima.reggie.filter;

import com.alibaba.fastjson.JSON;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 检查用户是否为登录
 */
@WebFilter(filterName = "loginCheckFilter",urlPatterns = "/*")
@Slf4j
public class LoginCheckFilter implements Filter {
    //路径匹配器，支持通配符
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse res = (HttpServletResponse) servletResponse;
        log.info("拦截到请求:{}",req.getRequestURI());
        //1、获取本次请求的uri
        String uri = req.getRequestURI();
        //不需要处理的请求路径
        String[] urls = new String[]{
                "/employee/login",
                "/employee/logout",
                "/backend/**",
                "/front/**",
                "/common/**",
                "/user/sendMsg",
                "/user/login"
        };
        //2、判断本次请求是否需要处理
        boolean check = check(urls,uri);
        //3、如果不需要处理，则直接放行
        if (check) {
            filterChain.doFilter(req, res);
            return;
        }
        //4、判断登录状态，如果已登录，则直接放行
        if(req.getSession().getAttribute("employee") != null){

            Long empId = (Long) req.getSession().getAttribute("employee");
            BaseContext.setCurrentId(empId);

            filterChain.doFilter(req,res);
            return;
        }
        if (req.getSession().getAttribute("user") != null){
            Long userId = (Long) req.getSession().getAttribute("user");
            BaseContext.setCurrentId(userId);

            filterChain.doFilter(req,res);
            return;
        }
        //5、如果未登录则返回未登录结果,通过输出流方式向客户端响应数据
        res.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
    }

    /**
     * 路径匹配，检查当前本次请求是否需要放行
     * @param uri
     * @return
     */
    public boolean check(String[] urls,String uri){
        for (String url:urls){
            boolean match = PATH_MATCHER.match(url,uri);
            if (match){
                return true;
            }
        }
        return false;
    }
}
