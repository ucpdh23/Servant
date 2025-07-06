package es.xan.servantv3.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.xan.servantv3.temperature.TemperatureVerticle;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class SchemaConverter {

    public static void main(String agrs[]) throws Exception {
        System.out.println(SchemaConverter.convertClassToSchema(TemperatureVerticle.Actions.SAVE.getPayloadClass()));
    }

    public static String convertClassToSchema(Class<?> clazz) throws Exception {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("properties", extractFields(clazz));
        schema.put("type", "object");

        ObjectMapper mapper = new ObjectMapper();
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema);
    }

    private static Map<String, Object> extractFields(Class<?> clazz) {
        var output = new LinkedHashMap<String, Object>();

        if (clazz == null) {
            return output;
        }

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            Class<?> type = field.getType();

            Map<String, Object> field_properties = new LinkedHashMap<>();
            if (isCustomClass(type)) {
                field_properties.put("type", "object");
                field_properties.put("properties", extractFields(type));
            } else if (type.isArray()) {
                field_properties.put("type", "array");
                // resolve type of array
                Class<?> array_items_type = type.getComponentType();
                field_properties.put("items", extractFields(array_items_type));
            } else if (isList(type)) {
                field_properties.put("type", "array");
                // resolve type of array
                Class<?> array_items_type = getListGenericType(field);
                field_properties.put("items", extractFields(array_items_type));
            } else if (type.isEnum()) {
                field_properties.put("type", "array");
                Map<String, Object> enum_property = new LinkedHashMap<>();
                enum_property.put("enum", extractEnumValues(type));
                field_properties.put("items", enum_property);
            } else {
                String typeName = mapType(field.getType());
                field_properties.put("type", typeName);
            }

            output.put(field.getName(), field_properties);
        }

        return output;
    }

    private static Class<?> getListGenericType(Field field) {
        if (!isList(field.getType())) {
            return null;
        }

        Type genericType = field.getGenericType();

        if (genericType instanceof ParameterizedType) {
            Type[] typeArgs = ((ParameterizedType) genericType).getActualTypeArguments();
            if (typeArgs.length == 1 && typeArgs[0] instanceof Class<?>) {
                return (Class<?>) typeArgs[0];
            }
        }

        return null; // Fallback if not determinable
    }

    private static Boolean isList(Class<?> type) {
        return List.class.isAssignableFrom(type);
    }

    private static List<String> extractEnumValues(Class<?> type) {
        if (!type.isEnum()) {
            return Collections.emptyList(); // Or throw an exception if preferred
        }

        Object[] constants = type.getEnumConstants();
        List<String> values = new ArrayList<>();

        for (Object constant : constants) {
            values.add(constant.toString()); // or ((Enum<?>) constant).name()
        }

        return values;
    }

    private static String mapType(Class<?> type) {
        if (type == String.class) return "string";
        if (type == int.class || type == Integer.class) return "int";
        if (type == long.class || type == Long.class) return "long";
        if (type == double.class || type == Double.class) return "double";
        if (type == float.class || type == Float.class) return "float";
        if (type == boolean.class || type == Boolean.class) return "boolean";
        if (type.isEnum()) return "enum";
        if (type.isArray() || Collection.class.isAssignableFrom(type)) return "array";
        return "object"; // default for custom or unknown types
    }



    private static boolean isCustomClass(Class<?> type) {
        return !(type.isPrimitive() || type.getName().startsWith("java.") || type.isEnum());
    }
}