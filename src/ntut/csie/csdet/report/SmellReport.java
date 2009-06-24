package ntut.csie.csdet.report;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmellReport {
	private static Logger logger = LoggerFactory.getLogger(SmellReport.class);
	
	//Report資料
	ReportModel model;

	SmellReport(ReportModel reportModel) {
		model = reportModel;
	}

	/**
	 * 產生Smell Report
	 */
	void build() {
		if (model != null) {
			//產生XML
			String xmlString = createXML();
			//利用XML把XSL內的欄位填上，並產生HTM檔
			createHTM(xmlString);
			//輸出HTM檔的Styles.css
			createStyles();
		}
	}

	/**
	 * 產生XML (供網頁參照資料)
	 * @return
	 */
	private String createXML() {
		Element root = new Element("EHSmellReport");
		Document myDocument = new Document(root);

		//把Summary資料加至XML Root
		printSummary(root);
		
		//把Code Information加至XML Root
		printCodeInfo(root);

		//把Packages總覽資料加至XML Root
		printAllPackageList(root);
		
		//把Package資料加至XML Root
		printPackageList(root);

		Format fmt = Format.getPrettyFormat();
		XMLOutputter xmlOut = new XMLOutputter(fmt);
		StringWriter writer = new StringWriter();
       	try {
       		//輸出XML
			xmlOut.output(myDocument, writer);
//			//印出XML至C糟 (Debug用)
//			FileWriter writeXML = new FileWriter("/myFile.xml");
//			xmlOut.output(myDocument, writeXML);
		} catch (IOException e) {
			logger.error("[IOException] EXCEPTION ", e);
		} finally {
			closeStingWriter(writer);
		}
		//把XML轉成字串
       	return writer.getBuffer().toString();
	}

	/**
	 * 把Summary資料加至XML Root
	 * @param root
	 */
	private void printSummary(Element root) {
		///Summary資料輸出///
		Element summary = new Element("Summary");
		summary.addContent(new Element("ProjectName").addContent(model.getProjectName()));
		summary.addContent(new Element("DateTime").addContent(model.getBuildTime()));
		summary.addContent(new Element("JPGPath").addContent("file:///" + model.getFilePath("Report.jpg", true)));
		if (model.isDerectAllproject()) {
			//若偵測全部則印出
			summary.addContent(new Element("Filter").addContent("[All Project]"));		
		} else {
			//若有條件印出條件
			if (model.getFilterList().size() != 0)
				summary.addContent(new Element("Filter").addContent(model.getFilterList().toString().replace("EH_STAR", "*")));
			//若沒有條件則印出沒有條件
			else
				summary.addContent(new Element("Filter").addContent("[No Package Select]"));			
		}
		root.addContent(summary);

		///EH Smell List資料輸出///
		Element smellList = new Element("EHSmellList");
		smellList.addContent(new Element("IgnoreCheckedException").addContent(String.valueOf(model.getIgnoreTotalSize())));
		smellList.addContent(new Element("DummyHandler").addContent(String.valueOf(model.getDummyTotalSize())));
		smellList.addContent(new Element("UnprotectedMainProgram").addContent(String.valueOf(model.getUnMainTotalSize())));
		smellList.addContent(new Element("NestedTryBlock").addContent(String.valueOf(model.getNestedTryTotalSize())));
		smellList.addContent(new Element("Total").addContent(String.valueOf(model.getTotalSmellCount())));
		root.addContent(smellList);
	}
	
	/**
	 * 把Code Information加至XML Root
	 * @param root
	 */
	private void printCodeInfo(Element root) {
		///Code Information List 資料輸出///
		Element codeInfoList = new Element("CodeInfoList");
		codeInfoList.addContent(new Element("LOC").addContent(String.valueOf(model.getTotalLine())));
		codeInfoList.addContent(new Element("TryNumber").addContent(String.valueOf(model.getTryCounter())));
		codeInfoList.addContent(new Element("CatchNumber").addContent(String.valueOf(model.getCatchCounter())));
		codeInfoList.addContent(new Element("FinallyNumber").addContent(String.valueOf(model.getFinallyCounter())));
		root.addContent(codeInfoList);
	}
	
	/**
	 * 把Packages總覽資料加至XML Root
	 * @param root
	 */
	private void printAllPackageList(Element root) {
		///AllPackage List資料輸出///
		Element allPackageList = new Element("AllPackageList");
		for (int i=0; i < model.getPackagesSize(); i++) {
			PackageModel packageModel = model.getPackage(i);

			Element allPackage = new Element("Package");
			//第一欄書籤連結和Package名稱
			if (packageModel.getPackageName() == "") {
				allPackage.addContent(new Element("HrefPackageName").addContent("#" + "(default_package)"));
				allPackage.addContent(new Element("PackageName").addContent("(default package)"));
			} else {
				allPackage.addContent(new Element("HrefPackageName").addContent("#" + packageModel.getPackageName()));
				allPackage.addContent(new Element("PackageName").addContent(packageModel.getPackageName()));
			}
			allPackage.addContent(new Element("IgnoreCheckedException")
								.addContent(String.valueOf(packageModel.getIgnoreSize())));
			allPackage.addContent(new Element("DummyHandler")
								.addContent(String.valueOf(packageModel.getDummySize())));
			allPackage.addContent(new Element("UnprotectedMainProgram")
								.addContent(String.valueOf(packageModel.getUnMainSize())));
			allPackage.addContent(new Element("NestedTryBlock")
								.addContent(String.valueOf(packageModel.getNestedTrySize())));
			allPackage.addContent(new Element("PackageTotal")
								.addContent(String.valueOf(packageModel.getTotalSmellSize())));
			allPackageList.addContent(allPackage);
		}
		///AllPackage List 總和資料輸出///
		Element total = new Element("Total");
		total.addContent(new Element("IgnoreTotal").addContent(String.valueOf(model.getIgnoreTotalSize())));
		total.addContent(new Element("DummyTotal").addContent(String.valueOf(model.getDummyTotalSize())));
		total.addContent(new Element("UnMainTotal").addContent(String.valueOf(model.getUnMainTotalSize())));
		total.addContent(new Element("NestedTrTotal").addContent(String.valueOf(model.getNestedTryTotalSize())));
		total.addContent(new Element("AllTotal").addContent(String.valueOf(model.getTotalSmellCount())));
		allPackageList.addContent(total);
		root.addContent(allPackageList);
	}
	
	/**
	 * 把Package資料加至XML Root
	 * @param root
	 */
	private void printPackageList(Element root) {
		//關係圖：
		//	PackageList
		//		- Package
		//			- PackageName
		//			- ClassList
		//				-SmellData(多個)
		//					- ClassName
		//					- MethodName
		//					- SmellType
		//					- Line
		//			- Total
		
		///Package List 資料輸出///
		Element packageList= new Element("PackageList");
		for (int i=0; i < model.getPackagesSize(); i++) {
			Element packages = new Element("Package");

			PackageModel pkTemp = model.getPackage(i);
			packages.addContent(new Element("PackageName").addContent(pkTemp.getPackageName()));
			Element classList = new Element("ClassList");
			for (int j=0; j<pkTemp.getClassSize(); j++) {
				ClassModel clTemp = pkTemp.getClass(j);
				//把Smell資訊加至ClassList
				if (clTemp.getSmellSize() > 0) {
					for (int k = 0; k < clTemp.getSmellSize(); k++) {
						Element smell = new Element("SmellData");
						smell.addContent(new Element("ClassName").addContent(clTemp.getClassName()));
						smell.addContent(new Element("State").addContent("0"));
						
						//欲連結的SourceCode資訊格式
						String codeLine = "#" + clTemp.getClassPath() + "#" + clTemp.getSmellLine(k) + "#";
						smell.addContent(new Element("LinkCode").addContent(codeLine));

						smell.addContent(new Element("MethodName").addContent(clTemp.getMethodName(k)));
						smell.addContent(new Element("SmellType").addContent(clTemp.getSmellType(k).replace("_", " ")));
						smell.addContent(new Element("Line").addContent(String.valueOf(clTemp.getSmellLine(k))));
						classList.addContent(smell);
					}
				//若Class內沒有Smell資料，印出Class名稱，並把Smell資訊設為"None"
				} else {
					Element smell = new Element("SmellData");
					smell.addContent(new Element("ClassName").addContent(clTemp.getClassName()));
					smell.addContent(new Element("State").addContent("1"));
					smell.addContent(new Element("MethodName").addContent("None"));
					smell.addContent(new Element("SmellType").addContent("None"));
					smell.addContent(new Element("Line").addContent("None"));
					classList.addContent(smell);
				}
			}
			packages.addContent(classList);
			packages.addContent(new Element("Total").addContent(String.valueOf(pkTemp.getTotalSmellSize())));
			
			packageList.addContent(packages);
		}
		root.addContent(packageList);
	}

	/**
	 * Close StringWriter
	 * @param writer
	 */
	private void closeStingWriter(StringWriter writer) {
		try {
			writer.close();
		} catch (IOException e) {
			logger.error("[IOException] EXCEPTION ", e);
		}
	}

	/**
	 * 輸出HTM檔的Styles.css
	 */
	void createStyles()
	{
		FileWriter fw = null;
		try {
			InputStream inputStyle = this.getClass().getResourceAsStream("/xslTemplate/styles.css");

			BufferedReader bReader = new BufferedReader(new InputStreamReader(inputStyle, "UTF-8"));
			
			File stylePath = new File(model.getFilePath("styles.css", false));
			
			//若沒有路徑就建立路徑
			if(!stylePath.exists()) {
				fw = new FileWriter(model.getFilePath("styles.css", false));
	
				//把讀取到的資料輸出
				String thisLine = null;
				while ((thisLine = bReader.readLine()) != null) {
					fw.write(thisLine);
				}
			}

		} catch (IOException ex) {
			logger.error("[IOException] EXCEPTION ",ex);
		} finally {
			if (fw != null)
				closeFileWriter(fw);
		}
	}

	/**
	 * Close FileWriter
	 * @param fw
	 */
	private void closeFileWriter(FileWriter fw) {
		try {
			fw.close();
		} catch (IOException e) {
			logger.error("[IOException] EXCEPTION ", e);
		}
	}
	
	/**
	 * 利用XML把XSL內的欄位填上，並產生HTM檔
	 * @param xmlString
	 */
	void createHTM(String xmlString) {
		try {
			InputStream inputStream = this.getClass().getResourceAsStream("/xslTemplate/sample.xsl");

			Source xslSource = new StreamSource(inputStream);

			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = null;
			transformer = tf.newTransformer(xslSource);
			Source xmlSource = new StreamSource(new StringReader(xmlString));

			FileOutputStream outputSteam = new FileOutputStream(model.getFilePath("sample.html", true));

			Result htmlResult = new StreamResult(outputSteam);
			transformer.transform(xmlSource, htmlResult);

			outputSteam.close();
		} catch (IOException ex) {
			logger.error("[IOException] EXCEPTION ",ex);
		} catch (TransformerConfigurationException ex) {
			logger.error("[Transformer Configuration Exception] EXCEPTION ",ex);
		} catch (TransformerException ex) {
			logger.error("[Transformer Exception] EXCEPTION ",ex);
		}
	}
}
