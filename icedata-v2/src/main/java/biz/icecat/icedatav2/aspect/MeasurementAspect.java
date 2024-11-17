package biz.icecat.icedatav2.aspect;

import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class MeasurementAspect {

    @Around("@annotation(measured)")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint, Measured measured) throws Throwable {

        long start = System.currentTimeMillis();

        Object result;

        String methodName = StringUtils.isNotBlank(measured.methodName()) ?
                measured.methodName() :
                joinPoint.getSignature().getName();

        result = joinPoint.proceed();

        long end = System.currentTimeMillis();
        log.info("{} completed in {} ms", methodName, (end - start));

        return result;
    }
}
