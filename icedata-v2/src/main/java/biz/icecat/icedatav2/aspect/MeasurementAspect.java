package biz.icecat.icedatav2.aspect;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

        log.info("Starting {}", methodName);

        result = joinPoint.proceed();

        long end = System.currentTimeMillis();

        int numberOfEntities = 0;
        if (result instanceof Integer number) {
            numberOfEntities = number;
        }
        log.info("{} completed, processed {} entries in {} ms", methodName, numberOfEntities, (end - start));

        return result;
    }
}
