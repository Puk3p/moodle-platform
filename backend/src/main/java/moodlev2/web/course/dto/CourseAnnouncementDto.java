package moodlev2.web.course.dto;

public record CourseAnnouncementDto(
        String title,
        String body,
        String meta,      //exemplu hgen in sensu asta ca pe siteuri "Posted 2 days ago"
        boolean isLast    //pt stling (borduri)
) {}