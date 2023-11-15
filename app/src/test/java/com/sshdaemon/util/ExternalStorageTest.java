package com.sshdaemon.util;

import static com.sshdaemon.util.ExternalStorage.getAllStorageLocations;
import static com.sshdaemon.util.ExternalStorage.getRootPath;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;

import org.junit.Test;

import java.io.File;

public class ExternalStorageTest {

    @Test
    public void testRootIsNotNull() {
        assertThat(getRootPath(), is("/"));
    }

    @Test
    public void testRootIsAlwaysInList() {
        var contextMock = mock(Context.class);
        when(contextMock.getExternalFilesDirs(any())).thenReturn(new File[0]);
        assertThat(getAllStorageLocations(contextMock).get(0), is("/"));
    }

    @Test
    public void testListIsPropagatedCorrectly() {
        var contextMock = mock(Context.class);
        when(contextMock.getExternalFilesDirs(any())).thenReturn(new File[]{new File("/foo/suffix"), new File("/bar/suffix")});
        assertThat(getAllStorageLocations(contextMock).get(0), is("/"));
    }
}