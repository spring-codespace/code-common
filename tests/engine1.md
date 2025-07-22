# Unit Tests for CAMT Report Generator Classes

Here's a comprehensive set of unit tests for the provided classes:

## Test for AbstractCamtReportGenerator

```java
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.validation.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

@ExtendWith(MockitoExtension.class)
public class AbstractCamtReportGeneratorTest {

    @Mock
    private JAXBContext jaxbContext;
    @Mock
    private Schema schema;
    @Mock
    private Marshaller marshaller;
    @Mock
    private JAXBElement<Object> documentElement;
    @Mock
    private List<ReportData> mockData;
    @Mock
    private ReportContext mockContext;

    private AbstractCamtReportGenerator<Object> generator;

    @BeforeEach
    void setUp() {
        generator = new AbstractCamtReportGenerator<Object>(Object.class, "test.xsd") {
            @Override
            protected JAXBElement<Object> createDocument(List<ReportData> data, ReportContext context) {
                return documentElement;
            }
        };

        // Use reflection to inject mocks for testing
        try {
            var jaxbContextField = AbstractCamtReportGenerator.class.getDeclaredField("jaxbContext");
            jaxbContextField.setAccessible(true);
            jaxbContextField.set(generator, jaxbContext);

            var schemaField = AbstractCamtReportGenerator.class.getDeclaredField("schema");
            schemaField.setAccessible(true);
            schemaField.set(generator, schema);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void generateReport_ShouldMarshalDocument() throws Exception {
        when(jaxbContext.createMarshaller()).thenReturn(marshaller);
        
        byte[] result = generator.generateReport(mockData, mockContext);
        
        assertNotNull(result);
        verify(marshaller).setSchema(schema);
        verify(marshaller).setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        verify(marshaller).marshal(documentElement, any(ByteArrayOutputStream.class));
    }

    @Test
    void generateReport_ShouldThrowReportGenerationException_WhenJAXBFails() throws Exception {
        when(jaxbContext.createMarshaller()).thenReturn(marshaller);
        doThrow(new JAXBException("test error")).when(marshaller).marshal(any(), any());
        
        assertThrows(ReportGenerationException.class, 
            () -> generator.generateReport(mockData, mockContext));
    }

    @Test
    void constructor_ShouldThrowReportInitializationException_WhenSchemaNotFound() {
        assertThrows(ReportInitializationException.class,
            () -> new AbstractCamtReportGenerator<Object>(Object.class, "nonexistent.xsd") {
                @Override
                protected JAXBElement<Object> createDocument(List<ReportData> data, ReportContext context) {
                    return null;
                }
            });
    }
}
```

## Test for Camt054V02Generator

```java
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import javax.xml.bind.JAXBElement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class Camt054V02GeneratorTest {

    @Mock
    private ReportDataV02Mapper reportDataV02Mapper;
    
    @Mock
    private ObjectFactory objectFactory;
    
    @Mock
    private BankToCustomerDebitCreditNotificationV02Mapper bankNotificationMapper;
    
    @Mock
    private List<ReportData> mockData;
    
    @Mock
    private ReportContext mockContext;
    
    @Mock
    private io.vikunalabs.engine.v02.jaxb.version02.BankToCustomerDebitCreditNotificationV02 genericNotification;
    
    @Mock
    private BankToCustomerDebitCreditNotificationV02 camtNotification;
    
    @Mock
    private Document document;
    
    @Mock
    private JAXBElement<Document> documentElement;
    
    @Mock
    private Logger logger;

    @InjectMocks
    private Camt054V02Generator generator;

    @Test
    void createDocument_ShouldMapDataAndCreateDocument() {
        // Setup mocks
        when(reportDataV02Mapper.createBankNotification(mockData, mockContext)).thenReturn(genericNotification);
        when(BankToCustomerDebitCreditNotificationV02Mapper.INSTANCE.mapToCamt054V02(genericNotification))
            .thenReturn(camtNotification);
        when(objectFactory.createDocument()).thenReturn(document);
        when(document.withBkToCstmrDbtCdtNtfctn(camtNotification)).thenReturn(document);
        when(objectFactory.createDocument(document)).thenReturn(documentElement);

        // Execute
        JAXBElement<Document> result = generator.createDocument(mockData, mockContext);

        // Verify
        assertSame(documentElement, result);
        verify(reportDataV02Mapper).createBankNotification(mockData, mockContext);
        verify(BankToCustomerDebitCreditNotificationV02Mapper.INSTANCE).mapToCamt054V02(genericNotification);
        verify(objectFactory).createDocument();
        verify(document).withBkToCstmrDbtCdtNtfctn(camtNotification);
        verify(objectFactory).createDocument(document);
        verify(logger).trace("Generated camt054v02 document: {}", document);
    }

    @Test
    void constructor_ShouldInitializeWithCorrectParameters() {
        Camt054V02Generator generator = new Camt054V02Generator(reportDataV02Mapper);
        // This is a bit tricky to test directly due to the file loading in parent constructor
        // We mainly want to verify the correct schema path is used
        assertNotNull(generator);
    }
}
```

## Test for BankToCustomerDebitCreditNotificationV02Mapper

```java
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class BankToCustomerDebitCreditNotificationV02MapperTest {

    private final BankToCustomerDebitCreditNotificationV02Mapper mapper = 
        Mappers.getMapper(BankToCustomerDebitCreditNotificationV02Mapper.class);

    @Test
    void mapToCamt054V02_ShouldMapAllFields() {
        // Create a sample input
        io.vikunalabs.engine.v02.jaxb.version02.BankToCustomerDebitCreditNotificationV02 input = 
            new io.vikunalabs.engine.v02.jaxb.version02.BankToCustomerDebitCreditNotificationV02();
        // Set up input properties as needed
        
        // Execute mapping
        BankToCustomerDebitCreditNotificationV02 result = mapper.mapToCamt054V02(input);
        
        // Verify mapping
        assertNotNull(result);
        // Add more specific assertions based on your mapping requirements
    }

    @Test
    void mapToCamt054V02_ShouldHandleNullInput() {
        assertNull(mapper.mapToCamt054V02(null));
    }
}
```

## Additional Test Utilities

You might want to add these to your test resources:

1. Create a test schema file (`test.xsd`) in `src/test/resources/schemas/` for the AbstractCamtReportGenerator test.
2. Add test data builders for ReportData and ReportContext to make tests more readable.

## Notes on Testing Approach:

1. **AbstractCamtReportGenerator**:
   - Used reflection to inject mocks since the real JAXBContext and Schema are created in constructor
   - Tested both success and failure scenarios for marshalling
   - Verified proper exception handling

2. **Camt054V02Generator**:
   - Mocked all dependencies to isolate the unit under test
   - Verified the correct sequence of operations and method calls
   - Checked logging behavior

3. **BankToCustomerDebitCreditNotificationV02Mapper**:
   - Tested with real mapper instance (MapStruct generates implementation)
   - Verified null handling
   - You should add more specific field mapping tests based on your actual mapping requirements

4. **Integration Considerations**:
   - Consider adding integration tests that verify the actual XML output against the schema
   - You might want to add tests for edge cases (empty data lists, null contexts, etc.)
   - Consider using XMLUnit for more sophisticated XML comparison in integration tests
