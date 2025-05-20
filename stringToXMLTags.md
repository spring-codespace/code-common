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
