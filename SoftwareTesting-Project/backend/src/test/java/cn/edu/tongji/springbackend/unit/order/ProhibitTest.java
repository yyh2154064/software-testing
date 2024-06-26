package cn.edu.tongji.springbackend.unit.order;

import cn.edu.tongji.springbackend.TestException;
import cn.edu.tongji.springbackend.service.ProfileService;
import io.qameta.allure.*;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static cn.edu.tongji.springbackend.util.CSVUtils.*;
import static cn.edu.tongji.springbackend.util.CSVUtils.updateBlock;
import static cn.edu.tongji.springbackend.util.PathUtil.TC_PATH_UNIT_ORDER;
import static cn.edu.tongji.springbackend.util.TimeUtils.getFormatter;

@SpringBootTest
@Transactional
public class ProhibitTest {
    /**
     * 被测对象
     */
    @Resource
    private ProfileService profileService;

    /**
     * 测试用例类
     */
    @Data
    @AllArgsConstructor
    private static class ProhibitTestCase {
        private String userId;
        private String ifProhibited;
    }

    /**
     * 一些常量，包括测试用例路径、特定内容的列号等
     */
    private static final String TEST_CASE_FILENAME = "prohibit.csv";
    private static final String TEST_CASE_RESULT_FILENAME = "prohibit_result.csv";
    private static final String TEST_PERSON = "2154064";
    private static final int COLUMN_USER_ID = 1;
    private static final int COLUMN_IF_PROHIBITED = 2;
    private static final int COLUMN_EXPECTED_OUTPUT = 3;
    private static final int COLUMN_ACTUAL_OUTPUT = 4;
    private static final int COLUMN_RESULT = 5;
    private static final int COLUMN_TIME = 7;
    private static final int COLUMN_PERSON = 8;

    /**
     * 测试时变量，包括读取进内存的csv表格、总用例数、已执行用例数等
     */
    private static List<String[]> data;
    private static int total = 0;
    private static int executed = 0;

    /**
     * 测试前置函数，通过读取csv文件返回测试用例对象列表。同时重置相关计数器
     * @return ProhibitTestCase列表
     */
    private static List<ProhibitTestCase> provideProhibitTestCases() {
        List<ProhibitTestCase> suite = new ArrayList<>();
        data = readCsv(TC_PATH_UNIT_ORDER + '/' + TEST_CASE_FILENAME);
        total = data.size();
        executed = 1;

        for (int i = 1; i < data.size(); i++) {
            suite.add(new ProhibitTest.ProhibitTestCase(data.get(i)[COLUMN_USER_ID], data.get(i)[COLUMN_IF_PROHIBITED]));
        }

        return suite;
    }

    @ParameterizedTest
    @MethodSource("provideProhibitTestCases")
    @Description("""
            - 管理员通过用户ID封禁用户账号
            - 用户id、是否封禁不能为空，且必须存在
            - 不能封禁已被封禁的账户
            - 不能解封未被封禁的账户
            - 不能封禁管理员账户
            """)
    @Epic("Order模块")
    @Feature("封禁用户")
    @Story("管理员根据用户ID更改账户状态")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("单元测试：封禁用户")
    @Owner("2154064")
    public void prohibitTest(ProhibitTestCase testCase) {
        String[] line = data.get(executed);  //获取测试用例csv文件中的当前行，方便填入内容
        String actualOutput;                 //实际输出

        //调取测试方法，获取实际输出
        try {
            int ifProhibit = Integer.parseInt(testCase.getIfProhibited());
            profileService.setUserProhibitedStatus(Integer.parseInt(testCase.getUserId()), ifProhibit == 1);
            actualOutput = "prohibit operation success";
        } catch (NumberFormatException e) {
            actualOutput = e.getMessage().subSequence(0, 20).toString();
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
            writeCsv(TC_PATH_UNIT_ORDER + '/' + TEST_CASE_RESULT_FILENAME, data);
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
