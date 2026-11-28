package com.uniwise.search_service.modules.redis.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class RedisServiceImplTest {

    @Mock
    private RedisTemplate<String, String> template;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RedisServiceImpl redisService;

    private static class TrackingCursor implements Cursor<String> {
        private final Iterator<String> delegate;
        private boolean closed = false;
        private long position = 0;

        TrackingCursor(List<String> keys) {
            this.delegate = keys.iterator();
        }

        @Override
        public long getCursorId() {
            return 0;
        }

        @Override
        public CursorId getId() {
            return CursorId.initial();
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public long getPosition() {
            return position;
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public String next() {
            position++;
            return delegate.next();
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteKeysByPatternUsesScanClosesCursorAndDeletesInBatches() {
        List<String> keys = IntStream.range(0, 250)
                .mapToObj(i -> "courses:search:page:" + i)
                .toList();
        TrackingCursor cursor = new TrackingCursor(keys);
        when(template.scan(any(ScanOptions.class))).thenReturn(cursor);

        redisService.deleteKeysByPattern("courses:search:*");

        ArgumentCaptor<ScanOptions> scanCaptor = ArgumentCaptor.forClass(ScanOptions.class);
        verify(template, times(1)).scan(scanCaptor.capture());
        assertEquals("courses:search:*", scanCaptor.getValue().getPattern());
        assertEquals(200L, scanCaptor.getValue().getCount());

        ArgumentCaptor<Collection<String>> deleteCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(template, times(3)).delete(deleteCaptor.capture());

        List<Collection<String>> deletedBatches = deleteCaptor.getAllValues();
        assertEquals(100, deletedBatches.get(0).size());
        assertEquals(100, deletedBatches.get(1).size());
        assertEquals(50, deletedBatches.get(2).size());

        List<String> allDeleted = new ArrayList<>();
        deletedBatches.forEach(allDeleted::addAll);
        assertEquals(keys, allDeleted);

        assertTrue(cursor.isClosed());
        verify(template, never()).keys(anyString());
    }

    @Test
    void deleteKeysByPatternClosesCursorAndSkipsDeleteWhenNoKeysMatch() {
        TrackingCursor cursor = new TrackingCursor(List.of());
        when(template.scan(any(ScanOptions.class))).thenReturn(cursor);

        redisService.deleteKeysByPattern("courses:empty:*");

        verify(template, times(1)).scan(any(ScanOptions.class));
        assertTrue(cursor.isClosed());
        verify(template, never()).delete(anyCollection());
        verify(template, never()).keys(anyString());
    }

    @Test
    void deleteKeysByPatternIgnoresNullOrBlankPattern() {
        redisService.deleteKeysByPattern(null);
        redisService.deleteKeysByPattern("   ");

        verify(template, never()).scan(any(ScanOptions.class));
        verify(template, never()).delete(anyCollection());
        verify(template, never()).keys(anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteKeysByPatternSkipsNullKeysFromCursorWithoutNpe() {
        List<String> keysWithNull = new ArrayList<>();
        keysWithNull.add("courses:search:page:0");
        keysWithNull.add(null);
        keysWithNull.add("courses:search:page:1");

        TrackingCursor cursor = new TrackingCursor(keysWithNull);
        when(template.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(template.delete(anyCollection())).thenReturn(2L);

        redisService.deleteKeysByPattern("courses:search:*");

        ArgumentCaptor<Collection<String>> deleteCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(template, times(1)).delete(deleteCaptor.capture());
        assertEquals(List.of("courses:search:page:0", "courses:search:page:1"),
                new ArrayList<>(deleteCaptor.getValue()));
        assertTrue(cursor.isClosed());
        verify(template, never()).keys(anyString());
    }
}
