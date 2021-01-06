package eu.xenit.care4alf.helpers.contentdata;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.alfresco.service.cmr.repository.ContentData;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

@Component
public class ContentDataHelper {

    public List<ContentDataComponent> getContentDataComponents(String contentDataQnameString,
            ContentData targetContentData) throws ClassNotFoundException {
        Class<?> contentDataClass = Class.forName("org.alfresco.service.cmr.repository.ContentData");
        List<ContentDataComponent> components = new ArrayList<>();
        List<Field> fieldList = new ArrayList<>();
        List<Method> getterMethodsList = new ArrayList<>();
        ReflectionUtils.doWithFields(contentDataClass, field -> fieldList.add(field));
        ReflectionUtils.doWithMethods(contentDataClass, method -> {
            if (method.getName().contains("get")) {
                getterMethodsList.add(method);
            }
        });
        for (Field field : fieldList) {
            if (!field.getName().equals("serialVersionUID") && !field.getName().equals("INVALID_CONTENT_URL_CHARS")) {
                ContentDataComponent component = new ContentDataComponent();
                component.setName(field.getName());
                String fieldQName = contentDataQnameString + ":" + field.getName();
                component.setQnamestring(fieldQName);
                for (Method method : getterMethodsList) {
                    Pattern regex = Pattern.compile(("get" + field.getName()), Pattern.CASE_INSENSITIVE);
                    Matcher matcher = regex.matcher(method.getName());
                    if (matcher.matches()) {
                        component.setValue(ReflectionUtils.invokeMethod(method, targetContentData));
                        break;
                    }
                }
                components.add(component);
            }
        }
        return components;
    }

}
