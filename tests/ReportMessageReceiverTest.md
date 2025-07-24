# Unit Tests for ReportMessageReceiver

Here's a comprehensive set of unit tests for the `ReportMessageReceiver` class with good coverage:

```java
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.jms.core.JmsTemplate;

@ExtendWith(MockitoExtension.class)
class ReportMessageReceiverTest {

    @Mock
    private MetricsCounterService metricsCounterService;
    
    @Mock
    private FeatureFlagService featureFlagService;
    
    @Mock
    private ReportProcessingPipeline reportProcessingPipeline;
    
    @Mock
    private ReportDeliveryManager reportDeliveryManager;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private Logger log;
    
    @InjectMocks
    private ReportMessageReceiver reportMessageReceiver;

    private ReportMessage testMessage;
    private String messageString = "test message";

    @BeforeEach
    void setUp() {
        testMessage = new ReportMessage();
        // Initialize testMessage with necessary data
        Report report = new Report();
        report.setId("12345");
        report.setType(ReportType.CAMT053);
        testMessage.setReport(report);
    }

    @Test
    void receiveReportMessage_shouldLogIncomingMessage() throws Exception {
        when(objectMapper.readValue(messageString, ReportMessage.class)).thenReturn(testMessage);
        when(featureFlagService.isFeatureFlagEnabled("sb-1363_read_camt053_messages_from_commander")).thenReturn(false);
        
        reportMessageReceiver.receiveReportMessage(messageString);
        
        verify(log).debug("Received a report message. Report Message: {}", messageString);
    }

    @Test
    void receiveReportMessage_shouldIncrementCounterWhenMessageReceived() throws Exception {
        when(objectMapper.readValue(messageString, ReportMessage.class)).thenReturn(testMessage);
        when(featureFlagService.isFeatureFlagEnabled("sb-1363_read_camt053_messages_from_commander")).thenReturn(false);
        
        reportMessageReceiver.receiveReportMessage(messageString);
        
        verify(metricsCounterService).incrementCommandReceivedCounter("CAMT053");
    }

    @Test
    void receiveReportMessage_shouldReturnEarlyWhenFeatureFlagEnabled() throws Exception {
        when(objectMapper.readValue(messageString, ReportMessage.class)).thenReturn(testMessage);
        when(featureFlagService.isFeatureFlagEnabled("sb-1363_read_camt053_messages_from_commander")).thenReturn(true);
        
        reportMessageReceiver.receiveReportMessage(messageString);
        
        verify(reportProcessingPipeline, never()).process(any());
        verify(reportDeliveryManager, never()).handleSuccessfulReport(any());
    }

    @Test
    void receiveReportMessage_shouldProcessMessageWhenFeatureFlagDisabled() throws Exception {
        ReportGenerationResult mockResult = mock(ReportGenerationResult.class);
        
        when(objectMapper.readValue(messageString, ReportMessage.class)).thenReturn(testMessage);
        when(featureFlagService.isFeatureFlagEnabled("sb-1363_read_camt053_messages_from_commander")).thenReturn(false);
        when(reportProcessingPipeline.process(testMessage)).thenReturn(mockResult);
        
        reportMessageReceiver.receiveReportMessage(messageString);
        
        verify(reportProcessingPipeline).process(testMessage);
        verify(reportDeliveryManager).handleSuccessfulReport(mockResult);
    }

    @Test
    void receiveReportMessage_shouldHandleNoPaymentException() throws Exception {
        when(objectMapper.readValue(messageString, ReportMessage.class)).thenReturn(testMessage);
        when(featureFlagService.isFeatureFlagEnabled("sb-1363_read_camt053_messages_from_commander")).thenReturn(false);
        when(reportProcessingPipeline.process(testMessage)).thenThrow(new NoPaymentException("No payments"));
        
        reportMessageReceiver.receiveReportMessage(messageString);
        
        verify(reportDeliveryManager).handleCancelReportProcessing(any(NoPaymentException.class));
    }

    @Test
    void receiveReportMessage_shouldHandleGenericException() throws Exception {
        Exception testException = new RuntimeException("Test error");
        
        when(objectMapper.readValue(messageString, ReportMessage.class)).thenReturn(testMessage);
        when(featureFlagService.isFeatureFlagEnabled("sb-1363_read_camt053_messages_from_commander")).thenReturn(false);
        when(reportProcessingPipeline.process(testMessage)).thenThrow(testException);
        
        reportMessageReceiver.receiveReportMessage(messageString);
        
        verify(reportDeliveryManager).handleReportProcessingError(testException, "CAMT053", "12345");
    }

    @Test
    void receiveReportMessage_shouldHandleUnknownReportWhenExceptionOccurs() throws Exception {
        Exception testException = new RuntimeException("Test error");
        ReportMessage invalidMessage = new ReportMessage(); // No report set
        
        when(objectMapper.readValue(messageString, ReportMessage.class)).thenReturn(invalidMessage);
        when(featureFlagService.isFeatureFlagEnabled("sb-1363_read_camt053_messages_from_commander")).thenReturn(false);
        when(reportProcessingPipeline.process(invalidMessage)).thenThrow(testException);
        
        reportMessageReceiver.receiveReportMessage(messageString);
        
        verify(reportDeliveryManager).handleReportProcessingError(testException, "unknown", "unknown");
    }

    @Test
    void receiveReportMessage_shouldHandleJsonParsingException() throws Exception {
        when(objectMapper.readValue(messageString, ReportMessage.class)).thenThrow(new JsonParseException(null, "Parse error"));
        
        reportMessageReceiver.receiveReportMessage(messageString);
        
        verify(reportDeliveryManager).handleReportProcessingError(any(), eq("unknown"), eq("unknown"));
    }
}
```

## Test Coverage Analysis

This test suite provides comprehensive coverage for the `ReportMessageReceiver` class:

1. **Happy Path Testing**:
   - Tests normal message processing when feature flag is disabled
   - Verifies proper counter incrementation

2. **Feature Flag Testing**:
   - Tests early return when feature flag is enabled
   - Tests normal processing when feature flag is disabled

3. **Error Handling**:
   - Tests handling of `NoPaymentException`
   - Tests handling of generic exceptions
   - Tests handling of exceptions with incomplete message data ("unknown" values)
   - Tests JSON parsing exceptions

4. **Edge Cases**:
   - Tests behavior with invalid/missing report data
   - Tests proper logging of incoming messages

5. **Verification Points**:
   - Verifies all external service interactions (metrics, feature flags, pipeline, delivery manager)
   - Verifies proper error handling paths
   - Verifies proper logging

The tests use Mockito to mock all dependencies and verify interactions, and JUnit 5 for the test framework. The `@ExtendWith(MockitoExtension.class)` enables Mockito annotations.

You might want to add more specific test cases if:
- There are more complex report types to test
- The error handling has more nuanced behavior
- The ReportMessage has more fields that affect processing
- There are additional feature flags to test
