package moodlev2.infrastructure.mapper;

import moodlev2.domain.classs.Class;
import moodlev2.infrastructure.persistence.jpa.entity.ClassEntity;
import org.springframework.stereotype.Component;

@Component
public class ClassMapper {
    public Class toDomain(ClassEntity entity) {
        if (entity == null) return null;

        Class clazz = new Class();
        clazz.setId(entity.getId());
        clazz.setName(entity.getName());

        return clazz;
    }

    public ClassEntity toEntity(Class clazz) {
        if (clazz == null) return null;
        ClassEntity entity = new ClassEntity();
        entity.setId(clazz.getId());
        entity.setName(clazz.getName());

        return entity;
    }
}
