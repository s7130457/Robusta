package ntut.csie.analyzer.nested;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.lang.reflect.Method;
import java.net.SocketTimeoutException;
import java.util.zip.DataFormatException;

import junit.framework.TestCase;
import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

public class NestedTryStatementExample {
	
	/**
	 * there is a nested try block in finally block.
	 */
	public void nestedFinally() {
		try {
			throwSocketTimeoutException();
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
		}
		finally {
			try {
				throwInterruptedIOException();
			} catch (InterruptedIOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * there is a nested try block in catch block.
	 * and the exception type between both catch block is in the same exception subset
	 * */
	public void nestedCatch_InnerCatchWithParentExceptionTypeOfOuter() {
		try {
			throwSocketTimeoutException();
		} catch (SocketTimeoutException e) {
			try {
				throwInterruptedIOException();
			} catch (InterruptedIOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}
	
	/**
	 * there is a nested try block in catch block.
	 * and the exception type between both catch block is not in the same exception subset
	 */
	public void nestedCatch_ExceptionOfTwoCatchWithoutParentChildRelations() {
		try {
			throwDataFormatException();
		} catch (DataFormatException e) {
			try {
				throwPropertyVetoException();
			} catch (PropertyVetoException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}
	
	/**
	 * there is a nested try block in catch block.
	 * and the exception type of inner catch block is the subset of outer catch block
	 */
	public void nestedCatch_InnerCatchWithChildExceptionTypeOfOuter() {
		try {
			throw new IOException();
		} catch (IOException e) {
			try {
				throw new FileNotFoundException();
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}
	
	/**
	 * there is nested try in finally block
	 * and the exception type between inner and outer catch block is not in the same exception subset
	 */
	public void nestedFinallyWithTopException() {
		try {
			throwDataFormatException();
		} catch (DataFormatException e) {
			e.printStackTrace();
		}
		finally {
			try {
				throwPropertyVetoException();
			} catch (PropertyVetoException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	@Robustness(value = { @RTag(level = 2, exception = java.net.SocketTimeoutException.class) })
	private void throwSocketTimeoutException() throws SocketTimeoutException {
		throw new SocketTimeoutException();
	}
	
	@Robustness(value = { @RTag(level = 2, exception = java.io.InterruptedIOException.class) })
	private void throwInterruptedIOException() throws InterruptedIOException {
		throw new InterruptedIOException();
	}
	
	@Robustness(value = { @RTag(level = 2, exception = java.util.zip.DataFormatException.class) })
	private void throwDataFormatException() throws DataFormatException {
		throw new DataFormatException();
	}
	
	@Robustness(value = { @RTag(level = 2, exception = java.beans.PropertyVetoException.class) })
	private void throwPropertyVetoException() throws PropertyVetoException {
		throw new PropertyVetoException(null, null);
	}
	
	public void nestedTry_usingTryCatchInseanIf() {
		InputStream is = null;
		String defaultPath = "/home/charles/default.txt";
		String firstChoosenPath = "/home/charles/123456.txt";
		try {
			try {
				is = new FileInputStream(firstChoosenPath);
			} catch (FileNotFoundException e) {
				is = new FileInputStream(defaultPath);
				is.read();
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	/* 
	 * ===============================================
	 * Following examples are the extended situations 
	 * which nested TryStatement is in other block.
	 * ===============================================
	 */
	
	/*
	 * Nested TryStatment in Try surrounded by other block.
	 */
	public void nestedTryInBlocks() {
		InputStream is = null;
		String defaultPath = "/home/charles/default.txt";
		String firstChoosenPath = "/home/charles/123456.txt";
		// IfStatement
		try {
			if(is == null) {
				try {
					is = new FileInputStream(firstChoosenPath);
				} catch (FileNotFoundException e) {
					is = new FileInputStream(defaultPath);
					is.read();
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		try {
			while(is == null) {
				try {
					is = new FileInputStream(firstChoosenPath);
				} catch (FileNotFoundException e) {
					is = new FileInputStream(defaultPath);
					is.read();
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		// WhileStatement
		try {
			while(is == null) {
				try {
					is = new FileInputStream(firstChoosenPath);
				} catch (FileNotFoundException e) {
					is = new FileInputStream(defaultPath);
					is.read();
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		//DoStatement
		try {
			do {
				try {
					is = new FileInputStream(firstChoosenPath);
				} catch (FileNotFoundException e) {
					is = new FileInputStream(defaultPath);
					is.read();
				}
			} while (is == null);
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		// ForStatement
		try {
			for(int i = 0; i<2; i++) {
				try {
					is = new FileInputStream(firstChoosenPath);
				} catch (FileNotFoundException e) {
					is = new FileInputStream(defaultPath);
					is.read();
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		// Block
		try {
			{
				try {
					is = new FileInputStream(firstChoosenPath);
				} catch (FileNotFoundException e) {
					is = new FileInputStream(defaultPath);
					is.read();
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Nested TryStatment in Catch surrounded by other block.
	 */
	public void nestedCatchInIfStatement_nestedCatchInWhileStatement() {
		InputStream is = null;
		try {
			is = new FileInputStream("/home/charles/download/abc.txt");
			is.read();
		} catch (FileNotFoundException e) {
			File alternativeFile1 = new File("/home/charles/download/a12.txt");
			if(alternativeFile1.exists()) {
				try {
					is = new FileInputStream(alternativeFile1);
					is.read();
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			e.printStackTrace();
		} catch (IOException e) {
			File alternativeFile2 = new File("/home/charles/download/123.txt");
			while (alternativeFile2.exists()){
				try {
					is = new FileInputStream(alternativeFile2);
					is.read();
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				} finally {
					if(alternativeFile2.exists()) {
						alternativeFile2.delete();
					}
				}
			}
			e.printStackTrace();
		}
	}

	public void nestedCatchInDoStatement_nestedCatchInBlock() {
		InputStream is = null;
		try {
			is = new FileInputStream("/home/charles/download/abc.txt");
			is.read();
		} catch (FileNotFoundException e) {
			File alternativeFile1 = new File("/home/charles/download/a12.txt");
			{
				try {
					is = new FileInputStream(alternativeFile1);
					is.read();
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			e.printStackTrace();
		} catch (IOException e) {
			File alternativeFile2 = new File("/home/charles/download/default.txt");
			do {
				try {
					is = new FileInputStream(alternativeFile2);
					is.read();
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				} finally {
					if(alternativeFile2.exists()) {
						alternativeFile2.delete();
					}
				}
			} while(alternativeFile2.exists());
			e.printStackTrace();
		}
	}
	
	public void nestedCatchInForStatement() {
		InputStream is = null;
		try {
			is = new FileInputStream("/home/charles/download/abc.txt");
			is.read();
		} catch (IOException e) {
			File alternativeFile2 = new File("/home/charles/download/default.txt");
			for(int i = 0; i<2; i++) {
				try {
					is = new FileInputStream(alternativeFile2);
					is.read();
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				} finally {
					if(alternativeFile2.exists()) {
						alternativeFile2.delete();
					}
				}
			}
			e.printStackTrace();
		}
	}
	
	/*
	 * Nested TryStatment in Finally surrounded by other block.
	 */
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public void nestedFinallyInIfStatement() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if(fis != null) {
				try {
					fis.close(); // Safe
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public void nestedFinallyInBlock() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if(fis == null) {
				//return;
			}
			try {
				fis.close(); // Safe
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public void nestedFinallyInWhileStatement() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			while(fis != null) {
				try {
					fis.close(); // Unsafe
				} catch (IOException e) {
					e.printStackTrace();
				}
				fis = null;
			}
		}
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public void nestedFinallyInDoStatement() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			do {	// This situation should never happened
				try {
					fis.close(); // Unsafe
				} catch (IOException e) {
					e.printStackTrace();
				}
				fis = null;
			} while (fis != null);
		}
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public void nestedFinallyInForStatement() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			for(int i = 0; i<2; i++) {	// This situation should never happened
				try {
					fis.close(); // Unsafe
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void nestedFinallyInNestedTry() {
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			for(int i = 0; i<2; i++) {	// This situation should never happened
				try {
					fis.close(); // Unsafe
				} catch (IOException e) {
					try {
						fos = new FileOutputStream("");
						fos.write(Byte.valueOf(e.getMessage()));
					} catch (FileNotFoundException e1) {
						throw new RuntimeException(e1);
					} catch (NumberFormatException e1) {
						throw new RuntimeException(e1);
					} catch (IOException e1) {
						throw new RuntimeException(e1);
					} finally {
						try {
							fos.close(); // Safe
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Example from Ant, this code is used to resolve the problem due to different version of JUnit
	 * Nested try in non-MethodDeclaration node.
	 * @author pig
	 */
    private static Method testCaseMethod = null;
    static {
        try {
        	testCaseMethod.isAccessible();
            testCaseMethod = TestCase.class.getMethod("getName", new Class[0]);
        } catch (NoSuchMethodException e) {
            // pre JUnit 3.7
            try {
                testCaseMethod = TestCase.class.getMethod("name", new Class[0]);
            } catch (NoSuchMethodException ignored) {
                // ignore
            }
        }
    }

	/**
	 * Nested try in constructor(It is also MethodDeclaration).
	 * content is the same as method 「nestedFinally」
	 * @author pig
	 */
    public NestedTryStatementExample() {
		try {
			throwSocketTimeoutException();
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
		} finally {
			try {
				throwInterruptedIOException();
			} catch (InterruptedIOException e) {
				e.printStackTrace();
			}
		}
    }
}
