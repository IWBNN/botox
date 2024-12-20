package com.botox.aspect;

import com.botox.controller.RoomApiController;
import com.botox.logger.RoomLogger;
import com.botox.logger.PostLogger;
import com.botox.logger.CommentLogger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
public class CustomLoggerAspect {

    @Around(
        "execution(* com.botox.controller.RoomApiController.leaveRoom(..)) || " +
        "execution(* com.botox.controller.RoomApiController.joinRoom(..)) || " +
        "execution(* com.botox.controller.RoomApiController.enterRoom(..))"
    )
    public Object logRoomActivity(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String methodName = joinPoint.getSignature().getName();
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        String action = getActionFromMethodName(methodName);
        Long roomNum = null;
        String roomContent = null;
        Long userId = null;

        for (Object arg : args) {
            if (arg instanceof Long) {
                if (roomNum == null) {
                    roomNum = (Long) arg;
                }
            } else if (arg instanceof String) {
                roomContent = (String) arg;
            } else if (arg instanceof RoomApiController.LeaveRoomForm) {
                userId = ((RoomApiController.LeaveRoomForm) arg).getUserId();
            } else if (arg instanceof RoomApiController.JoinRoomForm) {
                userId = ((RoomApiController.JoinRoomForm) arg).getUserId();
            } else if (arg instanceof RoomApiController.EnterRoomForm) {
                userId = ((RoomApiController.EnterRoomForm) arg).getUserId();
            }
        }

        RoomLogger.RoomLog(action, roomNum, roomContent, userId, request);

        return joinPoint.proceed();
    }

    @Around("execution(* com.botox.controller.PostController.reportPost(..))")
    public Object logPostReport(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Long postId = (Long) args[0];
        Object reportRequest = args[1];
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        PostLogger.logPostReport(
                "POST_REPORT",
                "REPORT",
                postId.toString(),
                getFieldValue(reportRequest, "reportingUserId"),
                getFieldValue(reportRequest, "reportingUserNickname"),
                getFieldValue(reportRequest, "reportedUserId"),
                getFieldValue(reportRequest, "reportedUserNickname"),
                getFieldValue(reportRequest, "reasonForReport"),
                request
        );

        return joinPoint.proceed();
    }

    @Around("execution(* com.botox.controller.CommentController.reportComment(..))")
    public Object logCommentReport(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Long commentId = (Long) args[0];
        Object reportForm = args[1];
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        CommentLogger.logCommentReport(
                "COMMENT_REPORT",
                "REPORT",
                commentId.toString(),
                getFieldValue(reportForm, "reportedContentId"),
                getFieldValue(reportForm, "reportingUserId"),
                getFieldValue(reportForm, "reportingUserNickname"),
                getFieldValue(reportForm, "reportedUserId"),
                getFieldValue(reportForm, "reportedUserNickname"),
                getFieldValue(reportForm, "reasonForReport"),
                request
        );

        return joinPoint.proceed();
    }

    private String getActionFromMethodName(String methodName) {
        if (methodName.startsWith("join")) return "join";
        if (methodName.startsWith("leave")) return "leave";
        if (methodName.startsWith("enter")) return "enter";
        return "other";
    }

    private String getFieldValue(Object obj, String fieldName) {
        try {
            return obj.getClass().getMethod("get" + capitalize(fieldName)).invoke(obj).toString();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}