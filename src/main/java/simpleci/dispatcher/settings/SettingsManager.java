package simpleci.dispatcher.settings;

import com.sun.org.apache.xpath.internal.operations.Bool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleci.dispatcher.entity.SettingsParameter;
import simpleci.dispatcher.repository.Repository;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsManager {
    private final static Logger logger = LoggerFactory.getLogger(SettingsManager.class);

    private final Repository repository;

    public SettingsManager(Repository repository) {
        this.repository = repository;
    }

    public Settings loadSettings() {
        Settings settings = new Settings();

        Map<String, String> sourceSettings = getSettings();

        Field[] fields = settings.getClass().getDeclaredFields();
        for(Field field : fields ) {
            Parameter parameter = field.getAnnotation(Parameter.class);
            if(parameter != null) {
                String parameterValue = sourceSettings.get(parameter.name());
                Object normalizedValue = normalizeValue(parameterValue, field);
                try {
                    field.set(settings, normalizedValue);
                } catch (IllegalAccessException e) {
                   logger.error("", e);
                }
            }
        }
        return settings;
    }

    private Object normalizeValue(String parameterValue, Field field) {
        Class<?> fieldClass = field.getType();
        if(fieldClass == String.class) {
            return  parameterValue;
        }else if(fieldClass == int.class) {
            return Integer.parseInt(parameterValue);
        }else if(fieldClass == boolean.class) {
            return parseBoolean(parameterValue);
        } else {
            return parameterValue;
        }
    }

    private boolean parseBoolean(String parameterValue) {
        if("1".equals(parameterValue)) {
            return true;
        }else if("0".equals(parameterValue)) {
            return false;
        } else {
            return Boolean.parseBoolean(parameterValue);
        }
    }

    private Map<String,String> getSettings() {
        List<SettingsParameter> settingParameters = repository.getSettings();
        Map<String, String> settings = new HashMap<>();
        for(SettingsParameter parameter : settingParameters) {
            settings.put(parameter.name, parameter.value);
        }
        return settings;
    }
}
