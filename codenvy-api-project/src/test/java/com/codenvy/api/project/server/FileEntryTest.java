/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2014] Codenvy, S.A. 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.project.server;

import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.VirtualFileSystemUser;
import com.codenvy.api.vfs.server.VirtualFileSystemUserContext;
import com.codenvy.api.vfs.server.impl.memory.MemoryMountPoint;
import com.google.common.io.ByteStreams;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author andrew00x
 */
public class FileEntryTest {
    private static final String      vfsUserName   = "dev";
    private static final Set<String> vfsUserGroups = new LinkedHashSet<>(Arrays.asList("workspace/developer"));

    private MemoryMountPoint mmp;
    private VirtualFile      myVfProject;
    private VirtualFile      myVfFile;
    private FileEntry        myFile;

    @BeforeMethod
    public void setUp() throws Exception {
        mmp = new MemoryMountPoint(null, new VirtualFileSystemUserContext() {
            @Override
            public VirtualFileSystemUser getVirtualFileSystemUser() {
                return new VirtualFileSystemUser(vfsUserName, vfsUserGroups);
            }
        });
        VirtualFile myVfRoot = mmp.getRoot();
        myVfProject = myVfRoot.createFolder("my_project");
        myVfProject.createFolder(".codenvy").createFile("project", null, null);
        myVfFile = myVfProject.createFile("test", "text/plain", new ByteArrayInputStream("to be or not to be".getBytes()));
        myFile = new FileEntry(myVfFile);
        Assert.assertTrue(myFile.isFile());
    }

    @Test
    public void testGetName() throws Exception {
        Assert.assertEquals(myFile.getName(), myVfFile.getName());
    }

    @Test
    public void testGetPath() throws Exception {
        Assert.assertEquals(myFile.getPath(), myVfFile.getPath());
    }

    @Test
    public void testGetMediaType() throws Exception {
        Assert.assertEquals(myFile.getMediaType(), myVfFile.getMediaType());
    }

    @Test
    public void testGetParent() throws Exception {
        Assert.assertEquals(myFile.getParent().getPath(), myVfProject.getPath());
    }

    @Test
    public void testGetContent() throws Exception {
        Assert.assertEquals(myFile.contentAsBytes(), "to be or not to be".getBytes());
    }

    @Test
    public void testGetContentAsStream() throws Exception {
        byte[] buf;
        try (InputStream inputStream = myFile.getInputStream()) {
            buf = ByteStreams.toByteArray(inputStream);
        }
        Assert.assertEquals(buf, "to be or not to be".getBytes());
    }

    @Test
    public void testSetMediaType() throws Exception {
        myFile.setMediaType("text/foo");
        Assert.assertEquals(myFile.getMediaType(), "text/foo");
    }

    @Test
    public void testUpdateContent() throws Exception {
        String mediaType = myFile.getMediaType();
        byte[] b = "test update content".getBytes();
        myFile.updateContent(b);
        Assert.assertEquals(myFile.contentAsBytes(), b);
        Assert.assertEquals(myFile.getMediaType(), mediaType);
    }

    @Test
    public void testUpdateContentWithStream() throws Exception {
        String mediaType = myFile.getMediaType();
        byte[] b = "test update content with stream".getBytes();
        OutputStream outputStream = myFile.openOutputStream();
        outputStream.write(b);
        outputStream.close();
        Assert.assertEquals(myFile.contentAsBytes(), b);
        Assert.assertEquals(myFile.getMediaType(), mediaType);
    }

    @Test
    public void testUpdateContentAndMediaType() throws Exception {
        String mediaType = "text/foo";
        byte[] b = "test update content and media type".getBytes();
        myFile.updateContent(b, mediaType);
        Assert.assertEquals(myFile.contentAsBytes(), b);
        Assert.assertEquals(myFile.getMediaType(), mediaType);
    }

    @Test
    public void testRename() throws Exception {
        String name = myFile.getName();
        String newName = name + "_renamed";
        String newPath = myVfProject.getVirtualFilePath().newPath(newName).toString();
        byte[] b = myFile.contentAsBytes();
        String mt = myFile.getMediaType();

        myFile.rename(newName);
        Assert.assertNull(myVfProject.getChild(name));
        Assert.assertNotNull(myVfProject.getChild(newName));
        Assert.assertEquals(myFile.getName(), newName);
        Assert.assertEquals(myFile.getPath(), newPath);
        Assert.assertEquals(myFile.getMediaType(), mt);
        Assert.assertEquals(myFile.contentAsBytes(), b);
    }

    @Test
    public void testMove() throws Exception {
        VirtualFile vfProject = mmp.getRoot().createFolder("my_project_2");
        vfProject.createFolder(".codenvy").createFile("project", null, null);
        String name = myFile.getName();
        String newPath = vfProject.getVirtualFilePath().newPath(name).toString();
        byte[] b = myFile.contentAsBytes();
        String mt = myFile.getMediaType();

        myFile.moveTo(vfProject.getPath());
        Assert.assertNull(myVfProject.getChild(name));
        Assert.assertNotNull(vfProject.getChild(name));
        Assert.assertEquals(myFile.getName(), name);
        Assert.assertEquals(myFile.getPath(), newPath);
        Assert.assertEquals(myFile.getMediaType(), mt);
        Assert.assertEquals(myFile.contentAsBytes(), b);
    }

    @Test
    public void testCopy() throws Exception {
        VirtualFile vfProject = mmp.getRoot().createFolder("my_project_2");
        vfProject.createFolder(".codenvy").createFile("project", null, null);
        String name = myFile.getName();
        String newPath = vfProject.getVirtualFilePath().newPath(name).toString();
        byte[] b = myFile.contentAsBytes();
        String mt = myFile.getMediaType();

        FileEntry copy = myFile.copyTo(vfProject.getPath());
        Assert.assertNotNull(myVfProject.getChild(name));
        Assert.assertNotNull(vfProject.getChild(name));
        Assert.assertEquals(copy.getName(), name);
        Assert.assertEquals(copy.getPath(), newPath);
        Assert.assertEquals(copy.getMediaType(), mt);
        Assert.assertEquals(copy.contentAsBytes(), b);
    }

    @Test
    public void testRemove() throws Exception {
        String name = myFile.getName();
        myFile.remove();
        Assert.assertFalse(myVfFile.exists());
        Assert.assertNull(myVfProject.getChild(name));
    }
}
