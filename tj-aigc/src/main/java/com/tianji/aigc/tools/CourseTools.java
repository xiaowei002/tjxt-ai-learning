package com.tianji.aigc.tools;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.tianji.aigc.config.ToolResultHolder;
import com.tianji.aigc.constants.Constant;
import com.tianji.aigc.tools.result.CourseInfo;
import com.tianji.api.client.course.CourseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CourseTools {

    private final CourseClient courseClient;
    private static final String FIELD_NAME_FORMAT = "{}_{}";  // 提取格式字符串常量


    /**
     * 定义工具方法，调用目前存在的方法
     *
     * @param courseId
     * @return
     */
    @Tool(description = Constant.Tools.QUERY_COURSE_BY_ID)
    public CourseInfo queryCourseById(@ToolParam(description = Constant.ToolParams.COURSE_ID) Long courseId, ToolContext toolContext) {
        return Optional.ofNullable(courseId)
                .map(id -> CourseInfo.of(courseClient.baseInfo(id, true)))
                .map(courseInfo -> {
                    // 存储数据的字段名course_123123123123123
                    String field = StrUtil.format(FIELD_NAME_FORMAT,
                            StrUtil.lowerFirst(CourseInfo.class.getSimpleName()),
                            courseInfo.getId());
                    //请求Id
                    var requestId = Convert.toStr(toolContext.getContext().get(Constant.REQUEST_ID));
                    ToolResultHolder.put(requestId, field, courseInfo);
                    return courseInfo;
                })
                .orElse(null);
    }
}
