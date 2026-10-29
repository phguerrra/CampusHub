package com.example.application;

import static org.junit.Assert.assertEquals;

import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Date;

public class MediaAndCommentModelTest {

    @Test
    public void commentModel_gettersAndSetters() {
        Timestamp now = new Timestamp(new Date());
        Comment comment = new Comment("c1", "event1", "user1", "João", "Excelente evento!", now);

        assertEquals("c1", comment.getId());
        assertEquals("event1", comment.getEventId());
        assertEquals("user1", comment.getUserId());
        assertEquals("João", comment.getAuthorName());
        assertEquals("Excelente evento!", comment.getText());
        assertEquals(now, comment.getCreatedAt());
    }

    @Test
    public void mediaModels_gettersAndSetters() {
        Timestamp now = new Timestamp(new Date());

        EventAudio audio = new EventAudio("a1", "event1", "user1", "Maria", "/path/audio.m4a", now);
        assertEquals("a1", audio.getId());
        assertEquals("event1", audio.getEventId());
        assertEquals("user1", audio.getUserId());
        assertEquals("Maria", audio.getAuthorName());
        assertEquals("/path/audio.m4a", audio.getAudioPath());
        assertEquals(now, audio.getCreatedAt());

        EventPhoto photo = new EventPhoto("p1", "event1", "user1", "Carlos", "/path/photo.jpg", now);
        assertEquals("p1", photo.getId());
        assertEquals("event1", photo.getEventId());
        assertEquals("user1", photo.getUserId());
        assertEquals("Carlos", photo.getAuthorName());
        assertEquals("/path/photo.jpg", photo.getPhotoPath());
        assertEquals(now, photo.getCreatedAt());

        EventVideo video = new EventVideo("v1", "event1", "user1", "Ana", "/path/video.mp4", now);
        assertEquals("v1", video.getId());
        assertEquals("event1", video.getEventId());
        assertEquals("user1", video.getUserId());
        assertEquals("Ana", video.getAuthorName());
        assertEquals("/path/video.mp4", video.getVideoPath());
        assertEquals(now, video.getCreatedAt());
    }
}
