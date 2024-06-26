package cn.edu.tongji.springbackend.unit.comment;

import cn.edu.tongji.springbackend.TestException;
import cn.edu.tongji.springbackend.dto.AddCommentRequest;
import cn.edu.tongji.springbackend.service.CommunicateService;
import io.qameta.allure.*;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static cn.edu.tongji.springbackend.util.CSVUtils.*;
import static cn.edu.tongji.springbackend.util.PathUtil.*;
import static cn.edu.tongji.springbackend.util.TimeUtils.*;

@SpringBootTest
@Transactional
public class AddCommentTest {
    /**
     * 被测对象
     */
    @Resource
    private CommunicateService communicateService;

    /**
     * 测试用例类
     */
    @Data
    @AllArgsConstructor
    private static class AddCommentTestCase {
        private String actId;
        private String userId;
        private String cmtContent;
        private String cmtTime;
    }

    /**
     * 一些常量，包括测试用例路径、特定内容的列号等
     */
    private static final String TEST_CASE_FILENAME = "add_comment.csv";
    private static final String TEST_CASE_RESULT_FILENAME = "add_comment_result.csv";
    private static final String TEST_PERSON = "2151294";
    private static final int COLUMN_USER_ID = 1;
    private static final int COLUMN_ACT_ID = 2;
    private static final int COLUMN_CMT_CONTENT = 3;
    private static final int COLUMN_CMT_TIME = 4;
    private static final int COLUMN_EXPECTED_OUTPUT = 5;
    private static final int COLUMN_ACTUAL_OUTPUT = 6;
    private static final int COLUMN_RESULT = 7;
    private static final int COLUMN_TIME = 9;
    private static final int COLUMN_PERSON = 10;

    /**
     * 测试时变量，包括读取进内存的csv表格、总用例数、已执行用例数等
     */
    private static List<String[]> data;
    private static int total = 0;
    private static int executed = 0;

    /**
     * 测试前置函数，通过读取csv文件返回测试用例对象列表。同时重置相关计数器
     * @return AddCommentTestCase列表
     */
    private static List<AddCommentTestCase> provideAddCommentTestCases() {
        List<AddCommentTestCase> suite = new ArrayList<>();
        data = readCsv(TC_PATH_UNIT_COMMENT + '/' + TEST_CASE_FILENAME);
        total = data.size();
        executed = 1;

        for (int i = 1; i < data.size(); i++) {
            String[] line = data.get(i);

            suite.add(new AddCommentTestCase(
                    line[COLUMN_ACT_ID],
                    line[COLUMN_USER_ID],
                    line[COLUMN_CMT_CONTENT],
                    line[COLUMN_CMT_TIME]
            ));
        }

        return suite;
    }

    @ParameterizedTest
    @MethodSource("provideAddCommentTestCases")
    @Description("""
            用户在社团的活动下方发表新的评论
            - 用户id、活动id不能为空，且必须存在
            - 评论内容不能为空，且长度小于1024字符
            - 评论时间输入必须符合格式：yyyy-MM-dd HH:mm:ss
            - 由于它是新建的评论，故父评论id为0，无需输入
            """)
    @Epic("Comment模块")
    @Feature("添加评论")
    @Story("用户在社团的活动下方发表新的评论")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("单元测试：添加评论")
    @Owner("2151294")
    @Sql(scripts = "/sql/comment_reset.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    public void addCommentTest(AddCommentTestCase testCase) {
        String[] line = data.get(executed);  //获取测试用例csv文件中的当前行，方便填入内容
        String actualOutput;                 //实际输出

        //调取测试方法，获取实际输出
        try {
            communicateService.addComment(new AddCommentRequest(
                    testCase.getCmtContent(),
                    LocalDateTime.parse(testCase.getCmtTime(), getFormatter()),
                    Integer.valueOf(testCase.getActId()),
                    Integer.valueOf(testCase.getUserId())
            ));

            actualOutput = "add comment success";
        } catch (NumberFormatException e) {
            actualOutput = e.getMessage().subSequence(0, 20).toString();
        } catch (DataIntegrityViolationException e) {
            actualOutput = e.getMessage().subSequence(6, 30).toString();
        } catch (Exception e) {
            actualOutput = e.getMessage();
        }

        //比对预期输出和实际输出
        boolean result = Objects.equals(actualOutput, line[COLUMN_EXPECTED_OUTPUT]);

        //填入实际输出、测试时间和测试人员
        updateBlock(data, executed, COLUMN_ACTUAL_OUTPUT, actualOutput);
        updateBlock(data, executed, COLUMN_TIME, LocalDateTime.now().format(getFormatter()));
        updateBlock(data, executed, COLUMN_PERSON, TEST_PERSON);

        //若执行到最后一行，将填入后的数据写入结果csv文件
        if (executed == total - 1)
            writeCsv(TC_PATH_UNIT_COMMENT + '/' + TEST_CASE_RESULT_FILENAME, data);
        else
            executed++;

        //根据比对结果填入测试结果，以及若不通过则直接抛出未通过异常，给后续报告捕获该信息
        if (result) {
            updateBlock(data, executed, COLUMN_RESULT, "通过测试");
        } else {
            updateBlock(data, executed, COLUMN_RESULT, "未通过测试");
            throw new TestException(executed, line[COLUMN_EXPECTED_OUTPUT], actualOutput);
        }
    }
}
