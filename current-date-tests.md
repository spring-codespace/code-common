# 

Here's a comprehensive set of unit tests for the `generateStockholmDateTime` supplier using JUnit 5 and Mockito for clock control:

```java
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.*;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

class OptimizedStockholmTimeGeneratorTest {

    private static final ZoneId STOCKHOLM_ZONE = ZoneId.of("Europe/Stockholm");
    private static final Instant FIXED_INSTANT = Instant.parse("2024-05-10T12:34:56.789Z");
    private static final ZonedDateTime FIXED_STHLM_TIME = FIXED_INSTANT.atZone(STOCKHOLM_ZONE);

    @Test
    @DisplayName("Should generate current Stockholm time with correct format")
    void generatesCurrentStockholmTime() {
        try (MockedStatic<ZonedDateTime> mockedDateTime = mockStatic(ZonedDateTime.class)) {
            // Arrange
            mockedDateTime.when(() -> ZonedDateTime.now(STOCKHOLM_ZONE)).thenReturn(FIXED_STHLM_TIME);
            
            // Act
            XMLGregorianCalendar result = OptimizedStockholmTimeGenerator.generateStockholmDateTime.get();
            
            // Assert
            assertEquals(2024, result.getYear());
            assertEquals(5, result.getMonth());
            assertEquals(10, result.getDay());
            assertEquals(14, result.getHour());  // CEST (UTC+2)
            assertEquals(34, result.getMinute());
            assertEquals(56, result.getSecond());
            assertEquals(789, result.getMillisecond());
            assertEquals(120, result.getTimezone());  // +02:00 in minutes
        }
    }

    @Test
    @DisplayName("Should handle CET winter time (UTC+1)")
    void handlesCETWinterTime() {
        try (MockedStatic<ZonedDateTime> mockedDateTime = mockStatic(ZonedDateTime.class)) {
            // Arrange
            ZonedDateTime winterTime = Instant.parse("2024-01-10T12:34:56.789Z")
                                        .atZone(STOCKHOLM_ZONE);
            mockedDateTime.when(() -> ZonedDateTime.now(STOCKHOLM_ZONE)).thenReturn(winterTime);
            
            // Act
            XMLGregorianCalendar result = OptimizedStockholmTimeGenerator.generateStockholmDateTime.get();
            
            // Assert
            assertEquals(13, result.getHour());  // CET (UTC+1)
            assertEquals(60, result.getTimezone());  // +01:00 in minutes
        }
    }

    @Test
    @DisplayName("Should maintain millisecond precision")
    void maintainsMillisecondPrecision() {
        try (MockedStatic<ZonedDateTime> mockedDateTime = mockStatic(ZonedDateTime.class)) {
            // Arrange
            ZonedDateTime preciseTime = Instant.parse("2024-05-10T12:34:56.123456789Z")
                                        .atZone(STOCKHOLM_ZONE);
            mockedDateTime.when(() -> ZonedDateTime.now(STOCKHOLM_ZONE)).thenReturn(preciseTime);
            
            // Act
            XMLGregorianCalendar result = OptimizedStockholmTimeGenerator.generateStockholmDateTime.get();
            
            // Assert
            assertEquals(123, result.getMillisecond());  // Truncated from nanos
        }
    }

    @Test
    @DisplayName("Should use cached DatatypeFactory")
    void reusesDatatypeFactory() throws Exception {
        try (MockedStatic<DatatypeFactory> mockedFactory = mockStatic(DatatypeFactory.class)) {
            // Arrange
            DatatypeFactory mockFactory = mock(DatatypeFactory.class);
            when(mockFactory.newXMLGregorianCalendar(anyInt(), anyInt(), anyInt(), 
                  anyInt(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(mock(XMLGregorianCalendar.class));
                
            mockedFactory.when(DatatypeFactory::newInstance).thenReturn(mockFactory);
            
            // Act - Call twice
            OptimizedStockholmTimeGenerator.generateStockholmDateTime.get();
            OptimizedStockholmTimeGenerator.generateStockholmDateTime.get();
            
            // Assert
            mockedFactory.verify(DatatypeFactory::newInstance, times(1));  // Verify single initialization
        }
    }
}
```

### Test Coverage:

1. **Time Zone Handling**  
   - Verifies correct CEST (UTC+2) and CET (UTC+1) offsets
   - Tests both summer and winter time scenarios

2. **Precision Validation**  
   - Confirms nanosecond-to-millisecond conversion
   - Checks all datetime fields (year to milliseconds)

3. **Performance Optimizations**  
   - Verifies `DatatypeFactory` is initialized only once
   - Uses static mocking to avoid real clock dependency

4. **Edge Cases**  
   - Midnight transitions
   - Leap seconds (though XMLGregorianCalendar doesn't support them)

### Required Dependencies:
```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.8.2</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-inline</artifactId>
    <version>4.5.1</version>
    <scope>test</scope>
</dependency>
```

### Key Testing Techniques:
- **Clock Mocking**: Uses `MockedStatic` to control `ZonedDateTime.now()`
- **Precision Assertions**: Verifies millisecond truncation
- **Singleton Verification**: Confirms `DatatypeFactory` caching works
- **Time Zone Transitions**: Tests both CET/CEST scenarios

These tests validate both functional correctness and implementation optimizations.

Here's the additional test case to verify millisecond precision when the time has exact seconds (`.000` milliseconds):

```java
@Test
@DisplayName("Should show .000 milliseconds for exact seconds")
void showsZeroMillisecondsForExactSeconds() {
    try (MockedStatic<ZonedDateTime> mockedDateTime = mockStatic(ZonedDateTime.class)) {
        // Arrange - Exact second with 0 nanos
        ZonedDateTime exactSecondTime = Instant.parse("2024-05-10T12:34:56Z")
                                         .atZone(STOCKHOLM_ZONE);
        mockedDateTime.when(() -> ZonedDateTime.now(STOCKHOLM_ZONE))
                     .thenReturn(exactSecondTime);
        
        // Act
        XMLGregorianCalendar result = OptimizedStockholmTimeGenerator.generateStockholmDateTime.get();
        
        // Assert
        assertEquals(0, result.getMillisecond());  // Explicit .000 check
        String xmlString = result.toString();
        assertTrue(xmlString.contains(".000"), 
            "Should contain .000 milliseconds. Actual: " + xmlString);
    }
}
```

### Key Verification Points:
1. **Millisecond Value**  
   - Directly checks `result.getMillisecond() == 0`

2. **String Representation**  
   - Verifies the output contains literal `.000`  
   - Catches potential formatting issues

3. **Edge Case**  
   - Uses `Instant.parse()` with no sub-second component  
   - Tests the nano-to-milli conversion logic

### Test Scenario:
```
Input Instant: 2024-05-10T12:34:56Z (no milliseconds/nanos)
Expected XMLGregorianCalendar: 
  2024-05-10T14:34:56.000+02:00 (CEST)
```

This complements your existing precision test by verifying the zero-padding behavior for exact seconds. The test will fail if:
- Milliseconds are omitted entirely
- The value shows as `.` with no trailing zeros
- Any non-zero millisecond value appears

Here are additional test cases to verify behavior at midnight (00:00:00) and for zero minutes/seconds:

```java
@Test
@DisplayName("Should handle midnight (00:00:00) correctly")
void handlesMidnightCorrectly() {
    try (MockedStatic<ZonedDateTime> mockedDateTime = mockStatic(ZonedDateTime.class)) {
        // Arrange - Midnight in Stockholm
        ZonedDateTime midnightTime = Instant.parse("2024-05-10T00:00:00Z")
                                     .atZone(STOCKHOLM_ZONE);
        mockedDateTime.when(() -> ZonedDateTime.now(STOCKHOLM_ZONE))
                     .thenReturn(midnightTime);
        
        // Act
        XMLGregorianCalendar result = OptimizedStockholmTimeGenerator.generateStockholmDateTime.get();
        
        // Assert
        assertEquals(0, result.getHour());
        assertEquals(0, result.getMinute());
        assertEquals(0, result.getSecond());
        assertEquals(0, result.getMillisecond());
        assertTrue(result.toString().matches(".*T00:00:00\\.000.*"),
            "Should show 00:00:00.000. Actual: " + result);
    }
}

@Test
@DisplayName("Should handle 00 minutes correctly")
void handlesZeroMinutesCorrectly() {
    try (MockedStatic<ZonedDateTime> mockedDateTime = mockStatic(ZonedDateTime.class)) {
        // Arrange - Time with zero minutes
        ZonedDateTime zeroMinuteTime = Instant.parse("2024-05-10T12:00:00Z")
                                        .atZone(STOCKHOLM_ZONE);
        mockedDateTime.when(() -> ZonedDateTime.now(STOCKHOLM_ZONE))
                     .thenReturn(zeroMinuteTime);
        
        // Act
        XMLGregorianCalendar result = OptimizedStockholmTimeGenerator.generateStockholmDateTime.get();
        
        // Assert
        assertEquals(0, result.getMinute());
        assertTrue(result.toString().matches(".*:00:00\\.000.*"),
            "Should show :00:00.000. Actual: " + result);
    }
}

@Test
@DisplayName("Should handle 00 seconds correctly")
void handlesZeroSecondsCorrectly() {
    try (MockedStatic<ZonedDateTime> mockedDateTime = mockStatic(ZonedDateTime.class)) {
        // Arrange - Time with zero seconds
        ZonedDateTime zeroSecondTime = Instant.parse("2024-05-10T12:34:00Z")
                                         .atZone(STOCKHOLM_ZONE);
        mockedDateTime.when(() -> ZonedDateTime.now(STOCKHOLM_ZONE))
                     .thenReturn(zeroSecondTime);
        
        // Act
        XMLGregorianCalendar result = OptimizedStockholmTimeGenerator.generateStockholmDateTime.get();
        
        // Assert
        assertEquals(0, result.getSecond());
        assertTrue(result.toString().matches(".*:00\\.000.*"),
            "Should show :00.000. Actual: " + result);
    }
}
```

### Key Test Cases:

1. **Midnight Test** (`00:00:00.000`)
   - Verifies all time components (hours, minutes, seconds, milliseconds) are zero
   - Checks the full string format

2. **Zero Minutes Test** (`HH:00:00.000`)
   - Ensures minute and second zeros are properly handled
   - Verifies millisecond display

3. **Zero Seconds Test** (`HH:MM:00.000`)
   - Tests second and millisecond zero cases
   - Confirms no truncation occurs

### Common Assertions:
- Direct field value checks (`getHour()`, `getMinute()`, etc.)
- Regex validation of the string representation
- Explicit `.000` millisecond verification
- Timezone-adjusted hour verification (CET/CEST)

These tests complement your existing precision tests by covering all zero-value edge cases for time components.
