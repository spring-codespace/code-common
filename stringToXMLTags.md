To efficiently extract all **non-empty** and **non-null** tags from an XML string in **Java**, the best approach is to use a proper XML parser like **DOM**, **SAX**, or **StAX** rather than regex. Among these, **StAX** (Streaming API for XML) is both **performance-optimized** and **memory-efficient**—suitable for large XML strings.

Here's an **optimized and maintainable Java solution using StAX**:

---

### ✅ Java Code (Using StAX):

```java
import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;
import java.io.StringReader;
import java.util.*;

public class NonEmptyXMLTagsExtractor {

    public static Set<String> getNonEmptyTags(String xmlContent) throws XMLStreamException {
        Set<String> nonEmptyTags = new HashSet<>();
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader reader = factory.createXMLEventReader(new StringReader(xmlContent));

        String currentElement = null;
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                currentElement = event.asStartElement().getName().getLocalPart();
            }

            if (event.isCharacters() && currentElement != null) {
                String text = event.asCharacters().getData().trim();
                if (!text.isEmpty() && !"null".equalsIgnoreCase(text)) {
                    nonEmptyTags.add(currentElement);
                }
            }

            if (event.isEndElement()) {
                currentElement = null;
            }
        }

        reader.close();
        return nonEmptyTags;
    }

    // Example usage
    public static void main(String[] args) throws XMLStreamException {
        String xml = "<root><name>John</name><age></age><city>null</city><country>USA</country></root>";
        Set<String> result = getNonEmptyTags(xml);
        System.out.println("Non-empty tags: " + result);
    }
}
```

---

### ✅ Output for Above Example:

```
Non-empty tags: [name, country]
```

---

### ✅ Why This Approach?

* **StAX is faster** and more memory-friendly than DOM for simple tasks like this.
* **Avoids regex**: which is error-prone with nested or malformed XML.
* **Maintainable**: Clear separation of logic, minimal dependencies.
* **Efficient**: Only necessary tags and values are processed.

---

If you need to return **tag-value pairs** instead of just tag names, let me know—I can tweak the logic easily.

# Yes I need to return tag-value pairs. Another question - does this code support attributes within the tag if exist?

Great — let’s handle both of your follow-up points:

---

## ✅ Updated Goals:

1. **Return tag–value pairs** (e.g., `"name" → "John"`).
2. **Support attributes** — if attributes exist, capture them **along with** the tag value.

---

## ✅ Enhanced Java Code (Returns Tag–Value Pairs & Includes Attributes):

```java
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import java.io.StringReader;
import java.util.*;

public class NonEmptyXMLTagsExtractor {

    public static Map<String, String> getNonEmptyTagValuePairs(String xmlContent) throws XMLStreamException {
        Map<String, String> tagValueMap = new LinkedHashMap<>();
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader reader = factory.createXMLEventReader(new StringReader(xmlContent));

        String currentTag = null;
        StringBuilder currentValue = new StringBuilder();
        Map<String, String> attributes = new LinkedHashMap<>();

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                currentTag = startElement.getName().getLocalPart();
                currentValue.setLength(0); // Reset value

                // Extract attributes if any
                attributes.clear();
                Iterator<Attribute> attrIterator = startElement.getAttributes();
                while (attrIterator.hasNext()) {
                    Attribute attr = attrIterator.next();
                    attributes.put(attr.getName().getLocalPart(), attr.getValue());
                }
            }

            if (event.isCharacters() && currentTag != null) {
                Characters characters = event.asCharacters();
                currentValue.append(characters.getData());
            }

            if (event.isEndElement()) {
                String value = currentValue.toString().trim();
                if (!value.isEmpty() && !"null".equalsIgnoreCase(value)) {
                    String key = currentTag;

                    // Append attributes (if any) to distinguish entries
                    if (!attributes.isEmpty()) {
                        key += attributes.entrySet().stream()
                                .map(e -> String.format("[@%s='%s']", e.getKey(), e.getValue()))
                                .reduce("", String::concat);
                    }

                    tagValueMap.put(key, value);
                }

                // Reset for next element
                currentTag = null;
                attributes.clear();
                currentValue.setLength(0);
            }
        }

        reader.close();
        return tagValueMap;
    }

    // Example usage
    public static void main(String[] args) throws XMLStreamException {
        String xml = "<root>" +
                        "<name>John</name>" +
                        "<age></age>" +
                        "<city>null</city>" +
                        "<country code=\"US\">USA</country>" +
                        "<language code=\"en\">English</language>" +
                     "</root>";

        Map<String, String> result = getNonEmptyTagValuePairs(xml);
        result.forEach((tag, value) -> System.out.println(tag + " = " + value));
    }
}
```

---

## ✅ Example Output:

```
name = John
country[@code='US'] = USA
language[@code='en'] = English
```

---

## ✅ Summary of Features:

| Feature                       | Supported?              |
| ----------------------------- | ----------------------- |
| Non-empty / non-"null" tags   | ✅                       |
| Tag–value pairs               | ✅                       |
| XML attributes                | ✅ (appended to tag key) |
| Performance + maintainability | ✅ (uses StAX)           |

---

If you want the attributes returned **separately** (not in the key string), or want to handle nested elements in a more structured way, I can further adapt it.

# Full Example - Optimized Code

You're asking for a **high-performance, manual-mapping XML parser** that:

* ✅ Extracts both **tag values** and **attributes**
* ✅ Handles **repeated tags** (e.g., `AdrLine`)
* ✅ Converts the parsed values into a **strongly typed object** (`PostalAddress6`)
* ✅ Uses an **efficient XML parser** (`StAX`)
* ✅ Avoids reflection for **max performance**

---

## ✅ Full Java Example

This solution includes:

1. A `StAX` parser that builds a `Map<String, List<String>>` with attribute support
2. A `buildPostalAddress()` method that maps those values to a `PostalAddress6` instance

---

### 📌 Step 1: PostalAddress6 class (simplified structure)

```java
import java.util.ArrayList;
import java.util.List;

public class PostalAddress6 {
    private String ctry;
    private String strtNm;
    private String bldgNb;
    private String pstCd;
    private String twnNm;
    private String ctrySubDvsn;
    private List<String> adrLine = new ArrayList<>();

    // Getters and Setters
    public String getCtry() { return ctry; }
    public void setCtry(String ctry) { this.ctry = ctry; }

    public String getStrtNm() { return strtNm; }
    public void setStrtNm(String strtNm) { this.strtNm = strtNm; }

    public String getBldgNb() { return bldgNb; }
    public void setBldgNb(String bldgNb) { this.bldgNb = bldgNb; }

    public String getPstCd() { return pstCd; }
    public void setPstCd(String pstCd) { this.pstCd = pstCd; }

    public String getTwnNm() { return twnNm; }
    public void setTwnNm(String twnNm) { this.twnNm = twnNm; }

    public String getCtrySubDvsn() { return ctrySubDvsn; }
    public void setCtrySubDvsn(String ctrySubDvsn) { this.ctrySubDvsn = ctrySubDvsn; }

    public List<String> getAdrLine() { return adrLine; }
}
```

---

### 📌 Step 2: XML Parsing & Object Mapping

```java
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import java.io.StringReader;
import java.util.*;

public class PostalAddressParser {

    public static void main(String[] args) throws XMLStreamException {
        String xml = """
            <PostalAddress6>
                <Ctry>US</Ctry>
                <AdrLine>123 Main St</AdrLine>
                <AdrLine>Suite 101</AdrLine>
                <StrtNm>Main Street</StrtNm>
                <PstCd>12345</PstCd>
                <TwnNm>New York</TwnNm>
                <BldgNb buildingType="residential">7A</BldgNb>
                <CtrySubDvsn>null</CtrySubDvsn>
            </PostalAddress6>
        """;

        Map<String, List<String>> values = parseXmlToMap(xml);
        PostalAddress6 address = buildPostalAddress(values);

        System.out.println("Country: " + address.getCtry());
        System.out.println("Street: " + address.getStrtNm());
        System.out.println("Building: " + address.getBldgNb());
        System.out.println("Address Lines: " + address.getAdrLine());
    }

    // Efficient XML tag+attribute parser
    public static Map<String, List<String>> parseXmlToMap(String xml) throws XMLStreamException {
        Map<String, List<String>> tagValueMap = new LinkedHashMap<>();
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader reader = factory.createXMLEventReader(new StringReader(xml));

        String currentTag = null;
        StringBuilder currentValue = new StringBuilder();
        Map<String, String> attributes = new LinkedHashMap<>();

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                StartElement start = event.asStartElement();
                currentTag = start.getName().getLocalPart();
                currentValue.setLength(0);
                attributes.clear();

                Iterator<Attribute> attrIter = start.getAttributes();
                while (attrIter.hasNext()) {
                    Attribute attr = attrIter.next();
                    attributes.put(attr.getName().getLocalPart(), attr.getValue());
                }
            }

            if (event.isCharacters() && currentTag != null) {
                currentValue.append(event.asCharacters().getData());
            }

            if (event.isEndElement()) {
                String val = currentValue.toString().trim();
                if (!val.isEmpty() && !"null".equalsIgnoreCase(val)) {
                    String key = buildTagKey(currentTag, attributes);
                    tagValueMap.computeIfAbsent(key, k -> new ArrayList<>()).add(val);
                }
                currentTag = null;
                currentValue.setLength(0);
                attributes.clear();
            }
        }

        reader.close();
        return tagValueMap;
    }

    private static String buildTagKey(String tagName, Map<String, String> attrs) {
        if (attrs.isEmpty()) return tagName;

        StringBuilder sb = new StringBuilder(tagName);
        for (Map.Entry<String, String> attr : attrs.entrySet()) {
            sb.append("[@").append(attr.getKey()).append("='").append(attr.getValue()).append("']");
        }
        return sb.toString();
    }

    // Manual mapping from parsed map to PostalAddress6
    public static PostalAddress6 buildPostalAddress(Map<String, List<String>> values) {
        PostalAddress6 addr = new PostalAddress6();

        for (Map.Entry<String, List<String>> entry : values.entrySet()) {
            String key = entry.getKey();
            List<String> valList = entry.getValue();
            String value = valList.get(0); // Only one value expected for single fields

            switch (stripAttributes(key)) {
                case "Ctry" -> addr.setCtry(value);
                case "StrtNm" -> addr.setStrtNm(value);
                case "BldgNb" -> addr.setBldgNb(value);
                case "PstCd" -> addr.setPstCd(value);
                case "TwnNm" -> addr.setTwnNm(value);
                case "CtrySubDvsn" -> addr.setCtrySubDvsn(value);
                case "AdrLine" -> addr.getAdrLine().addAll(valList);
                // Add more mappings as needed
            }
        }

        return addr;
    }

    // Removes attribute suffix from key like "BldgNb[@type='residential']"
    private static String stripAttributes(String key) {
        int idx = key.indexOf("[@");
        return (idx != -1) ? key.substring(0, idx) : key;
    }
}
```

---

## ✅ Output

```
Country: US
Street: Main Street
Building: 7A
Address Lines: [123 Main St, Suite 101]
```

---

## 🧠 Why This Is Optimized

| Feature        | Optimization                                        |
| -------------- | --------------------------------------------------- |
| **Parser**     | StAX → Streaming + low memory                       |
| **Mapping**    | Manual switch-case → no reflection overhead         |
| **Attributes** | Efficient attribute key generation only when needed |
| **Memory**     | Minimal object creation, no DOM                     |

---

## ⚡ Next Steps

Want to extend this with:

* Attribute **value injection** into the object?
* Nested JAXB support (e.g. `Party/PostalAddress6`)?
* JSON or XML generation from the object?

Let me know and I’ll tailor it further.

