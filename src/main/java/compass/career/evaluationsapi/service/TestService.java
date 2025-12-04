package compass.career.evaluationsapi.service;

import compass.career.evaluationsapi.dto.CreateTestRequest;
import compass.career.evaluationsapi.dto.TestListResponse;
import compass.career.evaluationsapi.dto.TestResponse;

import java.util.List;

public interface TestService {
    List<TestListResponse> getAllTests();
    TestResponse getTestDetails(Integer testId);
    TestResponse createTest(CreateTestRequest request);
    TestResponse updateTest(Integer testId, CreateTestRequest request);
}