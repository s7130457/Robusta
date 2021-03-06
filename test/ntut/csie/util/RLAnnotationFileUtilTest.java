package ntut.csie.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import ntut.csie.filemaker.JavaProjectMaker;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RLAnnotationFileUtilTest {
	private RLAnnotationFileUtil RLAnnotationFileUtil;
	JarFile answerJar = null;
	IWorkspace workspace;
	IWorkspaceRoot root;
	IProject project;

	private IJavaProject javaProj;
	JavaProjectMaker javaProjectMaker;
	
	@Before
	public void setUp() throws Exception {
		RLAnnotationFileUtil = new RLAnnotationFileUtil();
		String projectname = "MyProject";
		javaProjectMaker = new JavaProjectMaker(projectname);
		javaProjectMaker.setJREDefaultContainer();
		workspace = ResourcesPlugin.getWorkspace();
		root = workspace.getRoot();
		project = root.getProject(projectname);
		javaProj = JavaCore.create(project);
	}

	@Test
	public void testExtractJarIdTrue() {
		String sampleFullJarId = "D:/Timelog-v2.4.2-x64/123/ntut.csie.robusta.agile.exception.txt";

		String answerJarId = "ntut.csie.robusta.agile.exception.txt";
		String testJarId = "";
		try {
			Method testExtraJarIdMethod = RLAnnotationFileUtil.class
					.getDeclaredMethod("extractRLAnnotationJarId", String.class);
			testExtraJarIdMethod.setAccessible(true);
			Method setRLAnnotationJarId = RLAnnotationFileUtil.class
			.getDeclaredMethod("setRLAnnotationJarId", String.class);
			setRLAnnotationJarId.setAccessible(true);
	        setRLAnnotationJarId.invoke(RLAnnotationFileUtil, "ntut.csie.robusta.agile.exception");
			testJarId = (String) testExtraJarIdMethod.invoke(
					RLAnnotationFileUtil, sampleFullJarId);
		} catch (Exception ex) {
			throw new RuntimeException(ex);	
		}
		Assert.assertTrue(testJarId.equals(answerJarId));
	}

	@Test
	public void testExtractJarIdFalse() {
		String sampleFullJarId = "D:/Timelog-v2.4.2-x64/123";
		String answerJarId = "ntut.csie.robusta.agile.exception.txt";
		String testJarId = "";
		try {
			Method testExtraJarIdMethod = RLAnnotationFileUtil.class
					.getDeclaredMethod("extractRLAnnotationJarId", String.class);
			testExtraJarIdMethod.setAccessible(true);
			testJarId = (String) testExtraJarIdMethod.invoke(
					RLAnnotationFileUtil, sampleFullJarId);
		} catch (Exception ex) {
			throw new RuntimeException(ex);	
			}
		Assert.assertFalse(testJarId.equals(answerJarId));
	}
	
	@Test
	public void testCopyFileUsingFileStreams() {
		String workSpaceDir = System.getProperty("user.dir");
		File sampleFile = new File(workSpaceDir + "/copytest/copy.txt");
		if (!sampleFile.exists()) {
			sampleFile.getParentFile().mkdirs();
			try {
				sampleFile.createNewFile();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		File destFileDir = new File(workSpaceDir + "/copytest2/copy.txt");
		FileInputStream originFileInput =null;
		try {
			originFileInput = new FileInputStream(
					workSpaceDir + "/copytest/copy.txt");
			Method testCopyFileUSingFileStreamMethod = RLAnnotationFileUtil.class
					.getDeclaredMethod("copyFileUsingFileStreams",
							InputStream.class, File.class);
			testCopyFileUSingFileStreamMethod.setAccessible(true);
			testCopyFileUSingFileStreamMethod.invoke(RLAnnotationFileUtil,
					originFileInput, destFileDir);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}finally{
			try {
				originFileInput.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		File searFile = new File(workSpaceDir + "/copytest2/copy.txt");
		Assert.assertTrue(searFile.exists());
	}
	
	@Test
	public void testCheckExistInClassPath() {
		boolean statecheck = true;
		try {
			Method testCheckExistInClassPathMethod = RLAnnotationFileUtil.class
					.getDeclaredMethod("doesRLAnnotationExistInClassPath",
							IJavaProject.class);
			testCheckExistInClassPathMethod.setAccessible(true);
			Method setRLAnnotationJarId = RLAnnotationFileUtil.class
			.getDeclaredMethod("setRLAnnotationJarId", String.class);
			setRLAnnotationJarId.setAccessible(true);
			setRLAnnotationJarId.invoke(RLAnnotationFileUtil, "ntut.csie.robusta.agile.exception");
			statecheck = (Boolean) testCheckExistInClassPathMethod.invoke(RLAnnotationFileUtil, javaProj);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		Assert.assertFalse(statecheck);
	}
	
	@After
	public void TearDown() throws CoreException {
		RLAnnotationFileUtil = null;
		String workingSapaceDir = System.getProperty("user.dir");
		String filePath = workingSapaceDir + "/copytest/copy.txt";
		String filePath2 = workingSapaceDir + "/copytest2/copy.txt";
		deleteAll(filePath);
		deleteAll(filePath2);
		javaProjectMaker.deleteProject();
	}

	public void deleteAll(String path) {
		File fil = new File(path);
		fil.delete();
		File tmp = fil.getParentFile();
		tmp.delete();
	}
}
