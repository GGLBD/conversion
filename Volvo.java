package cov;

import java.io.*;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Row;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import util.*;
import xmlUtil.XSLTUtil;

public class Volvo extends Base implements IXMLFragReuse{
	
	
	/**
	 * Local
	 */
	//public static final String HomeDir = "\\\\dakota\\Archives\\Automation\\Volvo\\";
	//public static final String RawDataDir = "\\\\ranger\\data\\Automation\\Toyota\\Rawdata\\";
	//public static final boolean isOnline = false;
	/**
	 * Local
	 */
	
	
	/**
	 * System
	 */
	public static final String HomeDir = "\\\\dakota\\Archives\\Automation\\Volvo\\";
	public static final String RawDataDir = "\\\\ranger\\data\\Automation\\Toyota\\Rawdata\\";
	public static final boolean isOnline = false;
	/**
	 * System
	 */
	
	
	
	public static final String SPECIAL_TOOL_FILE = "VV_SpecialTools_2012";
	
    ////////////////////////////////Feb 2011/////////////////////////////////////////////////
	//public static String outputDirName = "C70Conv(-05)"; public static String VVModel = "C70 Conv (-05)";
	//public static String outputDirName = "V70(00-08)";	public static String VVModel = "V70 (00-08)";
	//public static String outputDirName = "C70Coupe(-02)";	public static String VVModel = "C70 Coupe (-02)";
	//public static String outputDirName = "S60(-09)";	public static String VVModel = "S60 (-09)";
	//public static String outputDirName = "V70XC(01-07)";	public static String VVModel = "V70 XC (01-07)";
	//public static String outputDirName = "S40(-04)";	public static String VVModel = "S40 (-04)";
	//public static String outputDirName = "S80(-06)";	public static String VVModel = "S80 (-06)";
	//public static String outputDirName = "V40";	public static String VVModel = "V40";

	
	////////////////////////////////Feb 2012/////////////////////////////////////////////////
	//public static String outputDirName = SPECIAL_TOOL_FILE;	public static String VVModel = SPECIAL_TOOL_FILE;
	//public static String outputDirName = "S40(04-)4";	public static String VVModel = "S40 (04-)";
	//public static String outputDirName = "V50";	public static String VVModel = "V50";
	//public static String outputDirName = "XC90";	public static String VVModel = "XC90";
	//public static String outputDirName = "V70(08-)";	public static String VVModel = "V70 (08-)";
	//public static String outputDirName = "C70(06-)";	public static String VVModel = "C70 (06-)";
	//public static String outputDirName = "XC70(08-)";	public static String VVModel = "XC70 (08-)";
	//public static String outputDirName = "S80(07-)";	public static String VVModel = "S80 (07-)";
	//public static String outputDirName = "C30";	public static String VVModel = "C30";
	
	////////////////////////////////Nov 2012/////////////////////////////////////////////////
	//public static String outputDirName = "S60(11-)";	public static String VVModel = "S60 (11-)";
	//public static String outputDirName = "XC60";	public static String VVModel = "XC60";
	
	//public static final String outputDirName = "C30";	public static final String VVModel = "C30";
	public static final String outputDirName = "V50";	public static final String VVModel = "V50";
	

	/////////////////////////////////////////////////////////////////////////
	static final String MEPS_DB = "MEPSP";
	//static final String MEPS_DB = "MEPST";
	//static final String fileNamePrefix = "VoLVo" + outputDirName + "-";
	static final String fileNamePrefix = "VoLVo_2" + outputDirName + "-";

	/////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////
	
	
	static MEPSUtil meps ;
	String year ;
	String model_fullName;
	static String model_short;
	//three qualifiergroups
	static final String PROD_SPEC = "Product specifications";
	static final String REPAIR = "Repair";
	static final String DIAGNOSTIC = "Diagnostic";
	//used to join qualify with id to generate fully qualified ID
	static final String IDJOIN = "-";
	
	//these fragment <testgrps> don't exist in the source fragment xml
	static List<String> badEles = Arrays.asList(
			"en-US0900c8af81a9c5d2#KC07234630",
			"en-US0900c8af80492be2#KC01404595",
			"en-US0900c8af8049d379#KC01480226",
			"en-US0900c8af8049d434#KC01539267",
			"en-US0900c8af8049d3ee#KC01525357",
			"en-US0900c8af8042d6f7#KC01550376",
			"en-US0900c8af80921d53#KC02606703",
			"en-US0900c8af850a9a72-KC07234630",
			"en-US0900c8af850a9a6c-KC07234630");
	
	
	/*these test procedure (xml fragment) exist in dbo.document table, but they are bad procedures since 
	 * their first <diagcallout> pointing to a non-existing xml fragment, they don't exist on Volov's web site either.*/
	static List<String> badProc = Arrays.asList(
			"en-US0900c8af837b1683",
			"en-US0900c8af8478a392",
			"en-US0900c8af8378dea2",
			"en-US0900c8af837a4ad2",
			"en-US0900c8af837a4408",
			"en-US0900c8af8380638f",
			"en-US0900c8af837b0f2a");
	
	//public static final int MAX_ARTICLE_SIZE = 900 * 1000; //0.9MB
	public static final int MAX_ARTICLE_SIZE = 1100 * 1000; 

	public static final String XMLLibDir = HomeDir + "lib\\xml\\";

	//destination dir
	public static final String outDir = HomeDir + "output\\";
	//source dir
	public static final String inDir = HomeDir + "in\\";
	public static final String tocDir = inDir + "TOC\\";
	
	/*
	 * Map to hold <refid, {document, elementType, elementText}> infoe.g.
	 * <RM000002X38001X-z0, {08GS350_01-00.xml, lst-itm, step 3}>
	 * <RM000002X38001X-z0, {08GS350_01-00.xml, info-obj, info-obj title}
	 */
	static Map<String, String[]> refMap = new HashMap<String, String[]>();
	static Map<String, String[]> refMapTemp = new HashMap<String, String[]>();
	boolean idCollected = false;
	
	//destination dir
	public static final String ModelOutDir = outDir + outputDirName + "\\";
	//gifs need imported to MEPS
	public static final String import2MEPSDir = HomeDir + "output\\importMEPS\\";
	//gifs need manually picked
	public static final String manual_pickupDir = HomeDir + "output\\manual_pickup\\";
	//script dir
	public static final String scriptDir = "src\\main\\resources\\Volvo\\";
	
	public static final String splitTocDir = HomeDir + "output\\" + outputDirName + "\\1_splitToc\\";
	public static final String fragInsertedDir = HomeDir + "output\\" + outputDirName + "\\2_fragInserted\\";
	public static final String tranformedANDInsertedGXInfoDir = HomeDir + "output\\" + outputDirName + "\\3_tranformedANDInsertedGXInfo\\";
	public static final String refSolvedDir = HomeDir + "output\\" + outputDirName + "\\4_refSolved\\";
	public static final String EBI_MAP_FILE= HomeDir + "output\\" + outputDirName + "\\ebiMap.csv";
	static Document DTCTables ;
	/*Map<oe_name, [<model1, fileFullName1>, <model2, fileFullName2>]>;
	e.g <E050367, <ES350, N:\Automation\Toyota\In\in\09ES350_RM10K0U_EN_10-03-19_UB\graphics\E050367.eps>>*/
	static Map<String, Map<String, String>> OENAMES = new HashMap<String, Map<String, String>>();
	static List<String> invalidGXs = new ArrayList<String>();
	//xml fragment to file name mapping; <RM000002X38001X.xml, 08GS350_01-00.xml>
	Map<String, String[]> fragMap = new HashMap<String, String[]>() ;

	 //<oename, caption)>
	static Map<String, String> caption_INFO;
	static Map<String, String> OE_INFO;
	
    static Map<String, Document> docs;
	public static enum Belong2Model {  ALL, PARTIAL, NONE	};
	
	
	public final static String SGMLHeaderIdentifer = "MITCHELL1//EN\">";
	// e.g. <!DOCTYPE SOM1 PUBLIC "-//MRIC//DTD SNAPON MITCHELL1//EN">
	public static String SGMLHeader;
	private final static Logger log = Logger.getLogger(Volvo.class.getName());
	public static String content;
	public static int count = 0;
	public static int idCount = 0;
	//static list which holds all child-parent relations, first item in String[] is child, second is parent
	private static List<String[]> mapList = new ArrayList<String[]>();
	//holds all diagnostic exclusive orphan ids with their titles for quick references
	static Map<String, String> orphan2Title = new HashMap<String, String>();
	static Connection con;
	public static String tempFileName;
	
	//a LRU cache (LRU Cache. LRU stands for "Least Recently Used", which refers to the policy of removing the oldest, or least-recently used entries to make space for new data.) 
	static final int cacheCapacity = 500;
	static Map<String, Document> fragCache = new LinkedHashMap<String, Document>(cacheCapacity+1, .75F, true) {
	    protected boolean removeEldestEntry(Map.Entry<String, Document> eldest) {
	    	return size() > cacheCapacity;  
	    }
	};
	
	
	//////////////////XML Fragement comparsion variables/////////////////
	/**  mapping from new fragment id to old fragment id, article GUID and <info-obj> id;
	 *  i.e <RM000000TV801AX, {RM000000T8X053X,A0012355,S12983948}>*/
	static Map<String, String[]> xmlFragMap;
	static String name = null;
	static List<String> dupLst = new ArrayList<String>();
	static List<String> diffLst = new ArrayList<String>();
	static List<String> dtcTypes;
	
	////////////////////////////////////////////////////////////////////
	
	
	public static void main(String[] args) throws Exception {
		
//		List<String> files = FileUtil.getAllFilesWithCertainExt(HomeDir + "Lib_Scrubbed\\xml\\", "xml", true);
//		//List<String> files = FileUtil.getAllFilesWithCertainExt(HomeDir + "temp\\xml\\", "xml", true);
//		for(int i=0; i<files.size(); i++){
//			String file = files.get(i);
//			pl("Scrubbing file:"+file);
//			Document doc;
//			try{
//				doc = XMLUtil.parseFile(file);
//			}catch (Exception e){
//				el("Failed to parse file:"+file);
//				continue;
//			}
//			NodeList texts = XMLUtil.xpathNodeSet(doc, "//text()[string-length(.) > 20]");
//			for(int j=0; j<texts.getLength(); j++){
//				String content = texts.item(j).getTextContent();
//				content = content.substring(0, 15) + "#@#@#" + (content.length() - 20) + "#" + content.substring(content.length()-5);
//				texts.item(j).setTextContent(content);
//			}
//			FileUtil.writer(file, XMLUtil.xmlToString(doc));
//		}
//		System.exit(0);
		
		
//		pl("writing file");
//		FileUtil.writer("testFile123.txt", "content");
//		System.exit(0);
		//exportXML();System.exit(0);
		//pl(getFullYearRange("1999,2001-2009"));System.exit(0);
		/*conntect2DB();
		Statement stmt = con.createStatement();
		pl("S:"+new Date());
		for(int i=0; i<100; i++){
			//String query = "select nevisid from dbo.document where chronicleid = 'chronicleid' or nevisid='refid' or vccNumber='vccnumbe'" ; 
			String query = "select nevisid from dbo.document where chronicleid = 'chronicleid' or vccNumber='vccnumbe'" ;
			//pl("refID="+refID+" query="+query);
			ResultSet rs = stmt.executeQuery(query);
		}
		pl("E:"+new Date());
		System.exit(0);*/
			
		/*String str = "2001,2004,2001,2003,1999, 1000,2002,2010";
		pl(sortCommaDelimitedString(str));
		System.exit(0);*/
		
		//exportXMLFrags2Lib();System.exit(0);
		//exportGXs2Lib();System.exit(0);
		//getTOC("C70 (06-)");System.exit(0);
		//Volvo vv = new Volvo("2012", VVModel);vv.init(); vv.collectSpecialToolsFrags();	System.exit(0);
		
		convert(VVModel);System.exit(0);

		
		/*List<String> files = FileUtil.getAllFilesWithCertainExt(HomeDir + "\\output\\C30test\\", "sgm", true);
		for(int i=0; i<files.size(); i++){
			String fileName = files.get(i);
			String newName = fileName.replace("VoLVoC30","VoLVoC301");
			new File(fileName).renameTo(new File(newName));
		}System.exit(0);*/
		
		
		/*List<String> files = FileUtil.getAllFilesWithCertainExt("N:\\MEPS\\Graphics\\Production\\", "jpg", true);
		count = 0;
		pl("files=" + files.size());
		for(int i=0; i<files.size(); i++){
			String fileName = files.get(i);
			if(fileName.contains("\\VV1")){
				count++;
				String newName = fileName.replace("\\VV1","\\LL1");
				new File(fileName).renameTo(new File(newName));
			}
		}
		pl("count=" + count);System.exit(0);
*/
		
		 /* ECM-2310 Injector 1. Signal to high. Permanent fault
		 * EFI-123, EFI-411 and EFI-251 or EFI-314. Faulty signal
		 */
		// Volvo vv = new Volvo("ALL", "C70 Conv (-05)");
		//vv.parseTiTitle("ECM-2310 Injector 1. Signal to high. Permanent fault", "ECM");

		

		//collectAllXMLRefs("C70 Conv (-05)");System.exit(0);
		//getAllXMLFragsFromToc(); System.exit(0);
		//preProcessCheck();System.exit(0);
		//convert();  System.exit(0);
	}
	
	public Volvo(String year, String model){
		this.year = year;
		this.model_fullName = model;
		//init();
	}
	
	/**
	 * entry method for converting toc.xml to MEPS article XMLs.
	 */
	public static void convert(String model) throws Exception{
		System.out.println("Starting too convert "+model + "  " + new Date());
		Volvo vv = new Volvo("ALL", model);
		//vv.loadCodeMap();System.exit(0);
		
		//*********Pre Process, give pcodes spreedsheet to editorial********//
		// vv.preConvertionProcess();				 System.exit(0);
		
		/******Start Conversion*******
		 *SQL Server; Caution! Make sure take Pcode spreedsheet from Editorial and put it in\PCodes\
		 * select id, ti_id, '' as pcode, code, replace(years, ',', ' ') years, replace(engines, ',', '') engines, title from [vv].[dtcCodes] where model = 'C70 Conv (-05)' and pcode = '' and codeType in ('ECM', 'EFI') order by code  
//		 */
		if(isOnline){
			conntect2DB();
		}

		
//		vv.init();
//		vv.splitModels(vv.model_fullName, splitTocDir);
//		vv.insertHeadInfo();
//		
//		vv.insertXMLFragments(); 
		tranform();
		//System.exit(0);
		vv.solveRefs();
		//System.exit(0);
		vv.eliminateNoTitleInfoObj();
		
		//need to recusively remove orphans
		int removed = 1; count = 1;
		while(removed>0){
			pl("@@@@@@@@@@Orpahn cleaning batch "+ count++);
			removed = vv.removeUnusedOrphans();
		}

		vv.toSGML();
		System.out.println("Number of invalid graphics found: "+invalidGXs.size());
		System.out.println("End of converting "+model + "  " + new Date());
	}
	
	private void init()throws Exception{
		System.out.println("Starting to initilization");
		FileUtil.flashDir(ModelOutDir);
		FileUtil.flashDir(splitTocDir);
		FileUtil.flashDir(fragInsertedDir);
		FileUtil.flashDir(tranformedANDInsertedGXInfoDir);
		FileUtil.flashDir(refSolvedDir);
		//year = model.substring(0,2);
		System.out.println("End of initilization");
	}
	
	/**
	 * pre conversion process against cleaned up TOC XML
	 * After this method is done, using the following SQL to create missing pcodes spreedsheet, give it to Editorial to manually populate these missing pcodes
	 * select id, ti_id, '' as pcode, code, replace(years, ',', ' ') years, replace(engines, ',', '') engines, title from [vv].[dtcCodes] where model = 'C70 Conv (-05)' and pcode = '' and codeType in ('ECM', 'EFI') order by code  
	 */
	void preConvertionProcess()throws Exception{
		System.out.println("Start of pre processing model "+this.model_fullName+" "+new Date());
		conntect2DB();
		
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Map<String, List<String>>> task = executor.submit(new Callable<Map<String, List<String>>>(){
			@Override public Map<String, List<String>> call() throws Exception {
				return getAllExclusiveDirectOrphans();
			}
		});
		
		String toc = tocDir + this.model_fullName + ".xml";
		//String toc = tocDir +  "cleaned_" + this.model_fullName + ".xml";
    	Document doc = XMLUtil.parseFile(toc);
    	
    	XMLUtil.xpathRemove(doc, "//ti_title[count(descendant::nevisid)=0]", "//FunctionGroup3[count(descendant::nevisid)=0]");
    	
    	Element docEle = doc.getDocumentElement();
    	mergeTI_TitleEles(docEle);
    	addYearRange2TI_TitleEles(docEle);
    	processNivisidYearRange(docEle);
    	
    	
    	//added 3/3/2019
    	//XMLUtil.xpathRemove(doc, "//FunctionGroup3[@group='NULL' and count(descendant::nevisid[documentType='servinfo'])=0]");
    	XMLUtil.xpathCommentOut(doc, "//FunctionGroup3[@group='NULL' and count(descendant::nevisid[@documentType='servinfo'])=0]");
    	XMLUtil.xpathAddAtt(doc, "notFirstLevelInfo", "true", "//nevisid[@documentType='servinfosub' and (count(following-sibling::nevisid[@documentType='servinfo']) > 0 or count(preceding-sibling::nevisid[@documentType='servinfo']) > 0)]");
    	XMLUtil.xpathCommentOut(doc, "//FunctionGroup2[count(descendant::nevisid)=0]");
    	
    	
    	pl("waiting for orphans retrieved");
    	Map<String, List<String>> eDOs = task.get();
		pl("exclusive orphans retrieved="+eDOs.size());
		Set<String> orphansInserted = new HashSet<>();
    	insertExclusiveDirectOrphans(eDOs, docEle, orphansInserted);
    	
    	for(String nevisid: orphansInserted){
    		XMLUtil.xpathCommentOut(doc, "//nevisid[@notFirstLevelInfo and @id='" + nevisid + "']");
    	}
    	//FileUtil.writer(tocDir + "temp_" + this.model_fullName + ".xml", XMLUtil.xmlToStringNoHeader(doc));
    	if(toc.contains("cleaned_")){
        	collectAllDTCs(docEle, false);
    	}else{
        	collectAllDTCs(docEle, true);
    	}
    	FileUtil.writer(tocDir + "cleaned_" + this.model_fullName + ".xml", XMLUtil.xmlToStringNoHeader(doc));
    	System.out.println("End of pre processing model "+this.model_fullName+" "+new Date());
	}
	
	/**
	 * Get TOC XML from database 
	 */
	public static void getTOC(String model)throws Exception{
		pl("Start of exporting all graphics to Lib "+new Date());
	  conntect2DB();
	  Statement stmt = con.createStatement();
	  String query = "select VV.getTOC('ALL', '" + model + "')";
	  ResultSet rs = stmt.executeQuery(query);
	  while (rs.next())
	  {	
		String content = rs.getString(1);
		FileUtil.writer(HomeDir + model + ".xml", content);
	  }
	}
	
	/**
	 * load pcodes info from excel into vv.dtcCodes table
	 */
	void updatePcodesInfo()throws Exception{
		System.out.println("Start updating Pcodes info");
		String pcodesDir = inDir + "PCodes\\";
		
		//Loading all pcodes from excel
		List<String> sqlStmts = new ArrayList<String>();
		InputStream inp = new FileInputStream(pcodesDir + this.model_fullName + " Pcodes.xls");
		HSSFWorkbook wb = new HSSFWorkbook(new POIFSFileSystem(inp));
		HSSFSheet sheet = wb.getSheet("pcodes");
		int rows = sheet.getLastRowNum();
		for(int i=0; i<=rows; i++){
			Row row = sheet.getRow(i);
			String id , pcodes = "";
			if(row.getCell(0) != null && row.getCell(1) != null){
				id = Math.round(row.getCell(0).getNumericCellValue()) + "";
				if(row.getCell(2) != null){
					pcodes = row.getCell(2).getStringCellValue();
					if(pcodes == null || pcodes.trim().equalsIgnoreCase("n/a") || pcodes.trim().equalsIgnoreCase("none")){
						pcodes = "";
					}else{
						pcodes = pcodes.trim();
					}
				}
				String sql = "update vv.dtcCodes set pcode ='" + pcodes + "' " +
				"where model = '" + this.model_fullName + "' and id = '" + id + "'";
				//pl("pcode sql="+sql);
				sqlStmts.add(sql);
			}
		}
		int affectedRows = SQLUtil.batchDML(con, sqlStmts);
		
		System.out.println("End of updating Pcodes info, affected rows = "+affectedRows);
	}
	/**
	 * get a list of article XMLs from revised TOC XML
	 */
	void splitModels(String tocFile, String destDir)throws Exception{
		System.out.println("Start of getting all article XMLs from TOC for "+"cleaned_" + this.model_fullName);
		conntect2DB();
		
		updatePcodesInfo();    //TODO resume me
		
		Statement stmt = con.createStatement();
		int count=1;
		ResultSet rs;
		

		
		//Step 1: get all TOC article XMLs
		String toc = tocDir + "cleaned_" + this.model_fullName + ".xml";
		//String toc = tocDir + "cleaned_" + this.model_fullName + ".xml";
    	Document doc = XMLUtil.parseFile(toc);
    	
	    NodeList nodes = XMLUtil.xpathNodeSet(doc, "//FunctionGroup2");
	    for(int i=0; i<nodes.getLength(); i++){
	    	Element node = (Element)nodes.item(i);
	    	
	    	String QualifierGroup = XMLUtil.xpathStr(node, "/ancestor::QualifierGroup/@name");
	    	if(QualifierGroup.equals(DIAGNOSTIC)){
	    		QualifierGroup = "Dia";
	    	}else if(QualifierGroup.equals(REPAIR)){
	    		QualifierGroup = "Rep";
	    	}else if(QualifierGroup.equals(PROD_SPEC)){
	    		QualifierGroup = "Prd";
	    	}else{
	    		throw new Exception("Unknown QualifierGroup found "+QualifierGroup);
	    	}
	    	String Qualifier = XMLUtil.xpathStr(node, "/ancestor::Qualifier/@name").replace("/", "_").trim();
	    	node.setAttribute("Qualifier", XMLUtil.xpathStr(node, "/ancestor::Qualifier/@name"));
	    	String group = node.getAttribute("group");
	    	String fileName = fileNamePrefix + (100 + count++) + "-" + QualifierGroup + "-" + Qualifier + "-" + group;
	    	fileName = fileName.replace(" ", "_");
	    	FileUtil.writer(destDir + fileName + ".xml", XMLUtil.xmlToStringNoHeader(node));	
	    }
	    pl(nodes.getLength() + " articles generated from TOC");
	    //System.exit(0);

		
		//Step 2: get all orphans XMLs
			//update vv.orphans table
		CallableStatement proc_stmt ;
		proc_stmt = con.prepareCall("{ call VV.getALLServGroupOrphans(?,?) }");
		proc_stmt.setString(1, "ALL");
		proc_stmt.setString(2, this.model_fullName);
		proc_stmt.execute();
		
		rs = stmt.executeQuery("select qualifiergroup, functiongroup1_title, functiongroup2_title, xmlContent " +
				"from VV.getALLOphranXMLs('ALL','" + this.model_fullName + "') " );
		while (rs.next()) {
			String qualifiergroup = rs.getString(1).trim() ;
			String functionGroup1_title = rs.getString(2).trim() ;
			String functionGroup2_title  = rs.getString(3) ;
			String xml  = rs.getString(4);
			String fileName;
			String docSubHead;
			//empty orphan articles
			if(!xml.contains("nevisid")){
				continue;
			}
			if(functionGroup2_title.equals("ALL")){
				docSubHead = functionGroup1_title.replace("/", "_").trim();
				fileName = fileNamePrefix + (100 + count++) + "-" + qualifiergroup.substring(0,3) + "-" + docSubHead ;
			}else{
				docSubHead = functionGroup1_title.replace("/", "_").trim() + "-" + functionGroup2_title.replace("/", "_").trim();
				fileName = fileNamePrefix + (100 + count++) + "-" + qualifiergroup.substring(0,3) + "-" + docSubHead;
			}
			xml = xml.replace("<orphans>", "<orphans><doc-subhead>" + docSubHead + "</doc-subhead>");
	    	fileName = fileName.replace(" ", "_");
			FileUtil.writer(destDir + fileName + ".xml", xml);	
		}


		//step 3: get all DTC index article XMLs
			//update vv.DTCIndex table
		proc_stmt = con.prepareCall("{ call VV.upsertALLDTCIndexXMLs(?) }");
		proc_stmt.setString(1, this.model_fullName);
		proc_stmt.execute();
		
		rs = stmt.executeQuery("select year, engine, codeType, xmlContent from vv.DTCIndex" +
				" where model = '" + this.model_fullName + "'  order by year, codeType");
		while (rs.next()) {
			String year = rs.getString(1).trim() ;
			String engine = rs.getString(2).trim().replace(" ", "") ;
			String codeType = rs.getString(3).trim() ;
			String xml  = rs.getString(4);
			FileUtil.writer(destDir + fileNamePrefix + (100 + count++) + "-" + "DTCIndex_" + year + "_" + codeType + "_" + engine + ".xml", xml);				
		}
		System.out.println("End of getting all article XMLs from Vovlo database, number of XMLs = "+count+ " retrieved in folder "+destDir);
	}
	
	/**
	 * parse ti_title's title with volvo code into a List, the first element is the title, the rest elements are codes
	 * ECM-2310 Injector 1. Signal to high. Permanent fault
	 * EFI-123, EFI-411 and EFI-251 or EFI-314. Faulty signal
	 */
	List<String> parseTiTitle(String title, String codeType){
		//pl("parsing title="+title);
		List<String> codes = new ArrayList<String>();
		String temp = title;
		String identifier = codeType+"-";
		while(temp.contains(identifier)){
			temp = temp.substring(temp.indexOf(identifier));
			String code;
			if(temp.contains(" ")){
				code = temp.substring(0,temp.indexOf(" "));
			}else{
				code = temp;
			}
			code = code.replace(",", "").replace(".", "");//remove possible tailing comma or period
			codes.add(code);
			temp = temp.replace(code, "");
			//pl("code="+code+" temp="+temp);
		}
		
		if(temp.startsWith(".") || temp.startsWith(",")){
			temp = temp.substring(1);
		}
		//pl("get title="+temp.trim());
		codes.add(0, temp.trim()); //this is the title
		return codes;
	}
	
	/**
	 * collect all DTC info to build DTC index articles 
	 */
	void collectAllDTCs(Element node, boolean makeUniqueIDs) throws Exception{
		pl("Start of collecting all DTC info");
		dtcTypes = new ArrayList<String>();
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery("select Type FROM [vv].[codeType]" );
		while (rs.next()) {
			dtcTypes.add(rs.getString(1)+"-");
		}
		
		//collect all volvo codes
		NodeList lst = XMLUtil.xpathNodeSet(node, "//ti_title");
		List<String> sqlStmts = new ArrayList<String>();
		List<String> sqlStmts2 = new ArrayList<String>();
		String deleteSQL = "delete from vv.dtcCodes where model = '" + this.model_fullName + "'";
		sqlStmts.add(deleteSQL);
		for(int i=0; i<lst.getLength(); i++){
			Element ti_title = (Element)lst.item(i);
			String title = ti_title.getAttribute("title").trim();
			if(title.length()<5){
				continue;
			}
			//ECM-3320 Ignition coil cylind
			String codeType = title.substring(0,3);
			String ti_id;
			if(makeUniqueIDs){
				//make a unique id for later use
				ti_id = ti_title.getAttribute("id")+ "-" + idCount++;
				ti_title.setAttribute("id", ti_id);
			}else{
				ti_id = ti_title.getAttribute("id");
			}
			if(dtcTypes.contains(title.substring(0,4))){
				//add one of BEI attributes
				ti_title.setAttribute("code.type", "title");
				String yearRange = ti_title.getAttribute("yearRange");
				
				//added on May 25, 2012
				yearRange = getFullYearRange(yearRange);
				
				String engines = ti_title.getAttribute("engine");
				String trans = ti_title.getAttribute("trans");
				String navTitle = ti_title.getAttribute("navTitle");
				
				List<String> codes = parseTiTitle(title, codeType);
				
				title = codes.get(0).replace("'", "''");
				//start with second element, the first element is title, not code
				for(int j=1; j<codes.size(); j++){
					String insertSQL = "INSERT INTO vv.dtcCodes (ti_id, code, model,years, codeType,engines,trans, title, navTitle) " +
					"VALUES ('"+ti_id+"','" + codes.get(j) + "','" + this.model_fullName + "','"+yearRange+"','"+codeType+"','"+engines+"','"+trans+"','" +title+"','"+ navTitle+"')";
					//pl("insertSQL="+insertSQL);
					sqlStmts.add(insertSQL);
				}
			}
		}
		SQLUtil.batchDML(con, sqlStmts);
		
		//collect all PCode
		sqlStmts.clear();
		//lst = XMLUtil.xpathNodeSet(node, "//nevisid[contains(@title,'Conversion table standardized diagnostic trouble codes (DTCs)/Volvo')]");
		lst = XMLUtil.xpathNodeSet(node, "//nevisid[contains(@title,'Conversion table standardized diagnostic trouble codes')]");
		for(int i=0; i<lst.getLength(); i++){
			Element nevisid = (Element)lst.item(i);
			Element nevisidFrag = getXMLFragments(nevisid.getAttribute("id"), null).getDocumentElement();
			String yearRangeStr = nevisid.getAttribute("yearRange");
			if(yearRangeStr.endsWith(",")){
				yearRangeStr = yearRangeStr.substring(0, yearRangeStr.lastIndexOf(","));
			}
			String[] yearRanges = yearRangeStr.split(",");
			String engineStr = nevisid.getAttribute("engine");
			if(engineStr.endsWith(",")){
				engineStr = engineStr.substring(0, engineStr.lastIndexOf(","));
			}
			String[] engines = engineStr.split(",");
			Element table = (Element)XMLUtil.xpathNode(nevisidFrag, "//table");
			NodeList rows = XMLUtil.xpathNodeSet(table, "//row");
			//start with second <row>, the first <row> is header info
			for(int j=1; j<rows.getLength(); j++){
				Element row = (Element)rows.item(j);
				String pcode = XMLUtil.xpathStr(row, "/entry[1]/ptxt[1]/text()");
				String vvCode = XMLUtil.xpathStr(row, "/entry[2]/ptxt[1]/text()");
				String faultType = XMLUtil.xpathStr(row, "/entry[4]/ptxt[1]/text()");

				//clean up faultType for later compare
				faultType = faultType.toLowerCase();
				faultType = faultType.replace(" ", "").replace(",", "").replace(".", "").replace(":", "");
				
				for(int j1=0; j1<yearRanges.length; j1++){
					for(int j2=0; j2<engines.length; j2++){
						String year = yearRanges[j1];
						String engine = engines[j2];
						String updateSQL = "update vv.dtcCodes " +
								"set pcode = ('" + pcode + "' + ',' + pcode) where code='" + vvCode + "' and years like '%" + year + 
								"%' and engines like '%" + engine + "%' and LOWER(replace(replace(replace(replace(title, ' ', ''), ',', ''), '.', ''), ':', '')) like " +
										"'%"+faultType+"%' and pcode not like '%" + pcode+ "%'";
						//pl("updateSQL="+updateSQL);
						sqlStmts.add(updateSQL);
						
						//give second try
						{faultType = faultType.replace("signaltoolow" , "signalmissing");
						faultType = faultType.replace("signalmissing" , "faultysignal");
						faultType = faultType.replace("sporadicsignal" , "signalsporadic");
						faultType = faultType.replace("a/cpressuresensor" , "(a/c)pressuresensor");}
						
						updateSQL = "update vv.dtcCodes " +
						"set pcode = ('" + pcode + "' + ',' + pcode) where code='" + vvCode + "' and years like '%" + year + 
						"%' and engines like '%" + engine + "%' and LOWER(replace(replace(replace(title, ' ', ''), ',', ''), '.', '')) like " +
								"'%"+faultType+"%' and pcode not like '%" + pcode+ "%'";
						//pl("updateSQL="+updateSQL);
						sqlStmts2.add(updateSQL);

					}
				}
			}
			SQLUtil.batchDML(con, sqlStmts);
			SQLUtil.batchDML(con, sqlStmts2);
		}
		pl("End of collect ALL DTCs, DTCs found "+sqlStmts.size());
	}
	
	/**
	 * insert exclusive orphan to the first nevisid only 
	 */
	void insertExclusiveDirectOrphans(Map<String, List<String>> eDOs, Element node, Set<String> orphansInserted) throws Exception{
		//exclusiveOrphan
		NodeList lst = XMLUtil.xpathNodeSet(node, "//nevisid[not(@notFirstLevelInfo)]");
		for(int i=0; i<lst.getLength(); i++){
			Element ele = (Element)lst.item(i);
			String id = ele.getAttribute("id");
			List<String> eDOList = eDOs.get(id);
			if(eDOList!=null){
				for(int j=0; j<eDOList.size(); j++){
					Element eDONode = ele.getOwnerDocument().createElement("exclusiveOrphan");
					eDONode.setAttribute("id", eDOList.get(j));
					ele.appendChild(eDONode);
					orphansInserted.add(eDOList.get(j));
				}
				//only add exclusive Orphans to the first parent 
				eDOs.remove(id);
			}
		}
	}
	
	/**
		get all exclusive direct orphans from database
 	 */
	Map<String, List<String>> getAllExclusiveDirectOrphans() throws Exception{
		pl(" start of getting all exclusive direct orphans from database");
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		
		CallableStatement proc_stmt ;
		proc_stmt = con.prepareCall("{ call VV.getExclusiveDirctOrphans(?,?) }");
		proc_stmt.setString(1, "ALL");
		proc_stmt.setString(2, this.model_fullName);
		proc_stmt.execute();
		
		
		Statement stmt = con.createStatement();
		String query = "select parentFrag, childFrag from vv.stg_exclusiveOrphans";
		pl("Executing query: "+query);
		ResultSet rs = stmt.executeQuery(query);
		while (rs.next()) {
			String parentFrag = rs.getString(1).trim() ;
			String orphanFrag = rs.getString(2).trim() ;
			if(!map.containsKey(parentFrag)){
				map.put(parentFrag, new ArrayList<String>());
			}
			map.get(parentFrag).add(orphanFrag);
		}
		return map;
	}
	
/*void splitModels(String destDir)throws Exception{
		System.out.println("Start of getting all article XMLs from Vovlo database");
		Statement stmt = con.createStatement();
		int count=1;
		ResultSet rs;
		
		//Step 1: get all article XMLs
		rs = stmt.executeQuery("select qualifiergroup, functiongroup1_title, functiongroup2_title, xmlContent " +
				"from VV.getArticleXMLs('ALL','" + VVModel + "') " +
				//"from VV.getArticleXMLs('ALL','V70 (00-08)') " +
				"where xmlContent is not null " +
				"order by qualifiergroup, functiongroup2_title");

		while (rs.next()) {
			String qualifiergroup = rs.getString(1).trim() ;
			String functionGroup1_title = rs.getString(2).trim() ;
			String functionGroup2_title  = rs.getString(3) ;
			String xml  = rs.getString(4);
			String fileName;
			String docSubHead;
			if(qualifiergroup.equals("Orphans")){
				//empty orphan articles
				if(!xml.contains("nevisid")){
					continue;
				}
				if(functionGroup2_title.equals("ALL")){
					docSubHead = functionGroup1_title.replace("/", "_").trim();
					fileName = qualifiergroup.substring(0,3) + "-" + docSubHead + "-" + ++count;
				}else{
					docSubHead = functionGroup1_title.replace("/", "_").trim() + "-" + functionGroup2_title.replace("/", "_").trim();
					fileName = qualifiergroup.substring(0,3) + "-" + docSubHead + "-" + ++count;
				}
				xml = xml.replace("<orphans>", "<orphans><doc-subhead>" + docSubHead + "</doc-subhead>");
			}else{
				xml = mergeTI_TitleEles(xml);	
				fileName = qualifiergroup.substring(0,3) + "-" + functionGroup2_title.replace("/", "_").trim() + "-" + ++count;
			}
			FileUtil.writer(destDir + fileName + ".xml", xml);	
		}
	
		//get all DTC index article XMLs
		rs = stmt.executeQuery("select year, engine, codeType, xmlContent from vv.DTCIndex" +
				" where model = '" + VVModel + "'  order by year, codeType");
		while (rs.next()) {
			String year = rs.getString(1).trim() ;
			String engine = rs.getString(2).trim() ;
			String codeType = rs.getString(3).trim() ;
			String xml  = rs.getString(4);
			FileUtil.writer(destDir + "DTCIndex_" + year + "_" + engine + "_" + codeType + "-" + ++count + ".xml", xml);				
		}
		System.out.println("End of getting all article XMLs from Vovlo database, number of XMLs = "+count+ " retrieved in folder "+destDir);
	}*/
	
	/**
	 * collect orphan titles info
	 */
	static Map<String, String> collectOrphanTitles(String xml)throws Exception{
		Map<String, String> map = new HashMap<String, String>();
		Document doc = XMLUtil.parseStr(xml);
		NodeList lst = XMLUtil.xpathNodeSet(doc, "//nevisid");
		for(int i=0; i<lst.getLength(); i++){
			Element ele = (Element)lst.item(i);
			String id = ele.getAttribute("id");
			String title = ele.getAttribute("title");
			map.put(id, title);
		}
		return map;
	}
	/**
	 *	 split orphan articles
	 */
	List<String> splitOrphanXML(String qualifiergroup, String xml)throws Exception{
		List<String> results = new ArrayList<String>();
		if(qualifiergroup.equalsIgnoreCase(DIAGNOSTIC)){
			results.add(xml);
		}else{
			results.add(xml);
		}
		/*Document doc = XMLUtil.parseStr(xml);
		NodeList funGroup3s = doc.getElementsByTagName("functiongroup3");
		for(int i=0; i<funGroup3s.getLength(); i++){
			Element funGroup3 = (Element)funGroup3s.item(i);
			int tiTitleCount = XMLUtil.xpathNodeSet(funGroup3, "/ti_title").getLength();
			for(int j=1; j<tiTitleCount; j++){
				Element tiTitle = (Element)XMLUtil.xpathNode(funGroup3, "/ti_title["+j+"]");
				if(tiTitle.hasAttribute("removeMe")){
					continue;
				}
				String navTitle = tiTitle.getAttribute("navTitle");
				String tiTitleStr = XMLUtil.xmlToString(tiTitle).replace(navTitle, "");
				NodeList tiTitles = XMLUtil.xpathNodeSet(funGroup3, "/ti_title[not(@removeMe) and position() > "+j+"]");
				for(int k=0; k<tiTitles.getLength(); k++){
					Element tiTitle1 = (Element)tiTitles.item(k);
					String navTitle1 = tiTitle1.getAttribute("navTitle");
					String tiTitleStr1 = XMLUtil.xmlToString(tiTitle1).replace(navTitle1, "");
					if(tiTitleStr.equals(tiTitleStr1)){
						navTitle += "," + navTitle1;
						tiTitle.setAttribute("navTitle", navTitle);
						tiTitle1.setAttribute("removeMe", "true");
					}
				}
			}
			//pl(XMLUtil.xpathRemove(funGroup3, "//ti_title[@removeMe]") + " <ti_title> removed");
			XMLUtil.xpathRemove(funGroup3, "//ti_title[@removeMe]");
		}*/
		return results;
	}
	
	/**
	 * add yearRange info to <nevisid> with duplicate title 
	 * Position sensor rear, headlamp => Position sensor rear, headlamp (2004-2007)
	 * 
	 */
	void processNivisidYearRange(Element node)throws Exception{
		mergeNivisidYearRange(node);
		addYearRange2Nivisid(node);
	}
	
	/**
	 * merge year ranges of duplicated nevisid (exactly same nevisid)
	 * <nevisid id="0900c8af850acb52" title="Bulb sun visor mirror (2004,2005,2006)" vccNumber="VCC-370400-1" yearRange="2004,2005,2006,"/>
       <nevisid id="0900c8af850acb52" title="Bulb sun visor mirror (2007)" vccNumber="VCC-370400-1" yearRange="2007,"/>
	 */
	void mergeNivisidYearRange(Element node)throws Exception{
		NodeList ti_titles = XMLUtil.xpathNodeSet(node, "//ti_title");
		for(int i=0; i<ti_titles.getLength(); i++){
			Element ti_title = (Element)ti_titles.item(i);
			NodeList nevisids = XMLUtil.xpathNodeSet(ti_title, "/nevisid");
			for(int j=0; j<nevisids.getLength(); j++){
				Element nevisid = (Element)nevisids.item(j);
				if(nevisid.hasAttribute("removeMe")){
					continue;
				}
				String id = nevisid.getAttribute("id");
				NodeList dups = XMLUtil.xpathNodeSet(nevisid, "/following-sibling::nevisid[@id='" + id + "']");
				for(int k=0; k<dups.getLength(); k++){
					Element dup = (Element)dups.item(k);
					dup.setAttribute("removeMe", "true");
					nevisid.setAttribute("yearRange", nevisid.getAttribute("yearRange") + dup.getAttribute("yearRange"));
				}
				String mergedYearRange = sortCommaDelimitedYears(nevisid.getAttribute("yearRange"), true, true);
				nevisid.setAttribute("yearRange", mergedYearRange);
			}
			XMLUtil.xpathCommentOut(ti_title, "/nevisid[@removeMe]");
		}
	}
	
	/**
	 * add yearRange info to <nevisid> which have different year range from its parent <it_title> 
	 * Position sensor rear, headlamp => Position sensor rear, headlamp (2004,2007)
	 */
	void addYearRange2Nivisid(Element node)throws Exception{
		NodeList ti_titles = XMLUtil.xpathNodeSet(node, "//ti_title");
		for(int i=0; i<ti_titles.getLength(); i++){
			Element ti_title = (Element)ti_titles.item(i);
			String tYearRange = ti_title.getAttribute("yearRange");
			NodeList nevisids = XMLUtil.xpathNodeSet(ti_title, "//nevisid");
			for(int j=0; j<nevisids.getLength(); j++){
				Element nevisid = (Element)nevisids.item(j);
				String nYearRange = nevisid.getAttribute("yearRange");
				if(!nYearRange.equalsIgnoreCase(tYearRange)){
					String title = nevisid.getAttribute("title");
					nevisid.setAttribute("title", title + " (" + nYearRange + ")");
				}
			}
		}
	}
	
	/**
	 * add yearRange info to <nevisid> with duplicate title 
	 * Position sensor rear, headlamp => Position sensor rear, headlamp (2004-2007)
	 */
	/*void addYearRange2Nivisid(Element node)throws Exception{
		NodeList ti_titles = XMLUtil.xpathNodeSet(node, "//ti_title");
		for(int i=0; i<ti_titles.getLength(); i++){
			Element ti_title = (Element)ti_titles.item(i);
			NodeList nevisids = XMLUtil.xpathNodeSet(ti_title, "/nevisid[ lower-case(preceding-sibling::nevisid[1]/@title)= lower-case(@title) or " +
					"lower-case(following-sibling::nevisid[1]/@title)= lower-case(@title)]");
			for(int j=0; j<nevisids.getLength(); j++){
				Element nevisid = (Element)nevisids.item(j);
				String title = nevisid.getAttribute("title");
				String yearRange = nevisid.getAttribute("yearRange");
				nevisid.setAttribute("title", title + " (" + yearRange + ")");
			}
		}
	}*/
	
	/**
	 * collect all <ti_title> children's year ranges, add them to <ti_title> 
	 */
	void addYearRange2TI_TitleEles(Element node)throws Exception{
		NodeList ti_titles = XMLUtil.xpathNodeSet(node, "//ti_title");
		for(int i=0; i<ti_titles.getLength(); i++){
			Element ti_title = (Element)ti_titles.item(i);
			NodeList nevisids = XMLUtil.xpathNodeSet(ti_title, "//nevisid");
			String yearRangeStr = "";
			for(int j=0; j<nevisids.getLength(); j++){
				Element nevisid = (Element)nevisids.item(j);
				yearRangeStr += nevisid.getAttribute("yearRange") + ",";
			}
			yearRangeStr = sortCommaDelimitedYears(yearRangeStr, true, true);

			String shortYearRange;
			if(yearRangeStr.contains(",")){
				//2000,2001,2002
				shortYearRange = yearRangeStr.substring(0,4) + "-" + yearRangeStr.substring(yearRangeStr.length()-4);
			}else{
				shortYearRange = yearRangeStr;
			}
 			ti_title.setAttribute("yearRange", yearRangeStr);
			ti_title.setAttribute("shortYearRange", shortYearRange);
		}
	}
	
	/**
	 * give an comma separated yearRange String, return a sorted, deduped yearRange String
	 * "2011,2000,2005,2005,"  => "2000,2005,2011" 
	 */
	/*private String getCleanYearRangeStr(String yearRangeStr){
		Set<String> yearRanges = new HashSet<String>();
		String[] tokens = yearRangeStr.split(",");
		for(int k=0; k<tokens.length; k++){
			String yr = tokens[k].trim();
			if(yr.length()>0){
				yearRanges.add(yr);
			}
		}
		Object[] tokenArr =yearRanges.toArray();
		Arrays.sort(tokenArr);
		String result = "";
		for(int i=0; i<tokenArr.length; i++){
			result += (String)tokenArr[i] + ",";
		}
		result = result.substring(0, result.lastIndexOf(","));
		return result;
	}*/
	
	/**
	 * P2000,P1999,P1999,P2001 => P1999,P2000,P2001
	 */
	static String sortCommaDelimitedString(String str){
		if(str.endsWith(",")){
			str = str.substring(0, str.length() - 1);
		}
		if(!str.contains(",")){
			return str;
		}
		String result= "";
		List<String> itemLst = new ArrayList<String>();
		String[] items = str.split(",");
		for(int k=0; k<items.length; k++){
			String token = items[k].trim();
			if(token.length()>0 && !itemLst.contains(token)){
				itemLst.add(token);
			}
		}
		Collections.sort(itemLst);
		for(String s:itemLst){
			result += s + ",";
		}
		result = result.substring(0, result.lastIndexOf(","));
		return result;
	}
	
	/**
	 * expand year range to full year range
	 * 1999-2001, 2003-2004 = > 2001,1999,2000,2003,2004  
	 */
	static String getFullYearRange(String str){
		if(str.contains("ALL")){
			return "ALL";
		}
		String result= "";
		String[] items = str.split(",");
		for(int k=0; k<items.length; k++){
			String yr = items[k].trim().replace(" " ,"");
			if(yr.length()>0){
				if(yr.contains("-")){
					int startYear = Integer.parseInt(yr.substring(0, yr.indexOf("-")));
					int endYear = Integer.parseInt(yr.substring(yr.indexOf("-")+1));
					result += startYear + "," + endYear + ",";
					for(int i=1, tempYear = startYear+1; tempYear<endYear; tempYear++){
						result += tempYear + ",";
					}
				}else{
					result += yr+",";
				}
			}
		}
		return sortCommaDelimitedYears(result, true, false);
	}
	
	/**
	 * 2001,1999,1999,2003, => 1999, 1999, 2001, 2003
	 * 2001,1999,2000,2003,2004 => 1999-2001, 2003-2004
	 */
	static String sortCommaDelimitedYears(String str, boolean removeDups, boolean replaceContinuousYearsWithYearRange){
		pl("sortCommaDelimitedYears:"+str);
		str = str.replace("20", ",20");
		if(!str.contains(",")){
			return str;
		}
		if(str.contains("ALL")){
			return "ALL";
		}
		String result= "";
		List<Integer> itemLst = new ArrayList<Integer>();
		String[] items = str.split(",");
		for(int k=0; k<items.length; k++){
			String yr = items[k].trim();
			if(yr.length()>0){
				
				if(yr.trim().equals("2011A")){yr = "2011";} //temp solution, needed to be addressed for both C30 and C70 (06-)
				if(yr.trim().equals("2012A")){yr = "2012";} //temp solution, needed to be addressed for both S60(11-)
				
				//pl("yr=="+yr);
				if(itemLst.contains(Integer.parseInt(yr)) && removeDups){
					//not need to add
				}else{
					itemLst.add(Integer.parseInt(yr));	
				}
			}
		}
		Collections.sort(itemLst);
		
		//1999,2000,2001,2003,2004 => 1999-2001, 2003-2004
		if(replaceContinuousYearsWithYearRange){
			Object[] years = itemLst.toArray();
			List<String> newYears = new ArrayList<String>(); 
			for(int i=0; i<years.length; i++){
				int newValue = ((Integer)years[i]).intValue();
				//neither first nor last one
				if(i>0 && i<years.length-1){
					if((newValue - ((Integer)years[i-1]).intValue())==1 &&
							(newValue - ((Integer)years[i+1]).intValue())==-1){
								newValue = 10001; //will be replaced by a dash later 
							}
				}
				newYears.add(newValue+"");
			}
			for(String s:newYears){
				result += s + ",";
				result = result.replace("10001,10001", "10001");
			}
			result = result.replace(",10001,", "-");
		}else{
			for(Integer s:itemLst){
				result += s.intValue() + ",";
			}
		}
		
		
		result = result.substring(0, result.lastIndexOf(","));
		result = result.replace(",", ", ");
		pl("done sortCommaDelimitedYears, result:"+result);
		return result;
	}
	
	static String mergeQualifiers(String qualifer, String newQualifer){
		String q = qualifer.toLowerCase().trim();
		String q2 = newQualifer.toLowerCase().trim();
		if(q2.length()==0){
			return qualifer;
		}
		if(q.startsWith(q2+";") || q.endsWith("; "+q2) || q.contains("; "+q2+";") || q.equals(q2)){
			return qualifer;
		}else{
			return qualifer+"; "+newQualifer;
		}
	}
	
	/**
	 *	 merge <ti_title> elements which are identical except attribute nav_title, which is engine and/or transmission type
	 */
	void mergeTI_TitleEles(Element node)throws Exception{
		pl("merging <ti_title> for functionGroup2 "+node.getAttribute("name"));
		NodeList funGroup3s = node.getElementsByTagName("FunctionGroup3");
		for(int i=0; i<funGroup3s.getLength(); i++){
			Element funGroup3 = (Element)funGroup3s.item(i);
			int tiTitleCount = XMLUtil.xpathNodeSet(funGroup3, "/ti_title").getLength();
			for(int j=1; j<tiTitleCount; j++){
				Element tiTitle = (Element)XMLUtil.xpathNode(funGroup3, "/ti_title["+j+"]");
				if(tiTitle.hasAttribute("removeMe")){
					continue;
				}
				NodeList tiTitles = XMLUtil.xpathNodeSet(funGroup3, "/ti_title[not(@removeMe) and position() > "+j+"]");
				for(int k=0; k<tiTitles.getLength(); k++){
					Element tiTitle1 = (Element)tiTitles.item(k);
					int hasSameChildren = hasSameChildren(tiTitle, tiTitle1);
					if(hasSameChildren==1){
						String navTitle = mergeQualifiers(tiTitle.getAttribute("navTitle"),tiTitle1.getAttribute("navTitle"));
						if(navTitle.trim().length()>2){
							tiTitle.setAttribute("navTitle", navTitle);
						}
						String engine = mergeQualifiers(tiTitle.getAttribute("engine"),tiTitle1.getAttribute("engine").trim());
						//not empty engine
						if(engine.length()>2){
							tiTitle.setAttribute("engine", engine);
						}
						String trans = mergeQualifiers(tiTitle.getAttribute("trans"),tiTitle1.getAttribute("trans").trim());
						//not empty tranmission
						if(trans.length()>2){
							tiTitle.setAttribute("trans", trans);
						}
						tiTitle1.setAttribute("removeMe", "true");
					}else if(hasSameChildren==-1){
						//not need to compare since they have different titles, titles are sorted
						break;
					}
				}
			}
			int count = XMLUtil.xpathRemove(funGroup3, "//ti_title[@removeMe]");
			pl(count + " duplicated <ti_title> elements removed");
		}
	}
	
	/**
	 * to check if both <ti_title> elements have same children
	 *  return -1 if titles are different
	 *  return 0 if titles are same but children are not
	 *  return 1 if they have both same titles and children
	 */
	int hasSameChildren(Element ele1, Element ele2)throws Exception{
		String title1 = ele1.getAttribute("title");
		String title2 = ele1.getAttribute("title");
		if(!title1.equalsIgnoreCase(title2)){
			return -1;
		}
		//further check
		Node n1 = ele1.cloneNode(true);
		Node n2 = ele2.cloneNode(true);
		NamedNodeMap attrs = n1.getAttributes();
		while (attrs.getLength() > 0) {
		    attrs.removeNamedItem(attrs.item(0).getNodeName());
		}
		attrs = n2.getAttributes();
		while (attrs.getLength() > 0) {
		    attrs.removeNamedItem(attrs.item(0).getNodeName());
		}

		
		//String id1 = ele1.getAttribute("id");
		//String id2 = ele2.getAttribute("id");
		
/*	String navTitle1 = ele1.getAttribute("navTitle");
		String engine1 = ele1.getAttribute("engine");
		String trans1 = ele1.getAttribute("trans");
		String id1 = ele1.getAttribute("id");
		String navTitle2 = ele2.getAttribute("navTitle");
		String engine2 = ele2.getAttribute("engine");
		String trans2 = ele2.getAttribute("trans");
		String id2 = ele2.getAttribute("id");
		String tiTitle1 = XMLUtil.xmlToStringNoHeader(ele1).replace(navTitle1, "").replace(engine1, "").replace(trans1, "").replace(id1, "");
		String tiTitle2 = XMLUtil.xmlToStringNoHeader(ele2).replace(navTitle2, "").replace(engine2, "").replace(trans2, "").replace(id2, "");
	*/	
		String tiTitle1 = XMLUtil.xmlToStringNoHeader(n1);
		String tiTitle2 = XMLUtil.xmlToStringNoHeader(n2);

		if(tiTitle1.equalsIgnoreCase(tiTitle2)){
			return 1;
		}else{
			//System.out.printf("id1=%s;tiTitle1=%s\n", id1,tiTitle1);
			//System.out.printf("id2=%s;tiTitle2=%s\n", id2,tiTitle2);
			return 0;
		}
	}
	
	/**
	 * give a model, collect all references based on TOCBuilder table, put them into fragMap table
	 * Note that this method will truncate the fragMap table first
	 * "C70 Conv (-05)"
	 */
	static void collectAllXMLRefs(String model) throws Exception{
		System.out.println("Start of getting all XML Refs");
		conntect2DB();
		String frag = null;
		int count = -1;
		CallableStatement stmt = con.prepareCall("{ call [VV].[getNextFrag](?, ?)}");
	    stmt.registerOutParameter(2, java.sql.Types.NCHAR);
	    
	    CallableStatement insertStmt = con.prepareCall("{ call [VV].[insertFragMap](?, ?)}");
	    do {
		    stmt.setNString(1, model);
		    stmt.execute();
		    frag = stmt.getNString(2);
		    pl(++count + "-Processing fragment "+ frag);
		    String fragFile;
		    NodeList refs;
		    List<String> ids = new ArrayList<String>();
		    if(frag != null){
		    	frag = frag.trim();
			    if(frag.equalsIgnoreCase("TOC")){
			    	fragFile = tocDir +  "2000_C70-Conv_TOC.xml";
			    	pl("fragFile="+fragFile);
			    	Document doc = XMLUtil.parseFile(fragFile);
				    refs = XMLUtil.xpathNodeSet(doc, "//NevisId");
				    //refs = XMLUtil.xpathNodeSet(doc, "//QualifierGroup[@name='Repair']//NevisId");
				    for(int i=0; i<refs.getLength(); i++){
				    	String ref = ((Element)refs.item(i)).getAttribute("id");
				    	ids.add(ref);
				    }
			    }else{
			    	fragFile = XMLLibDir + Util.getThreeLayerDir(frag) + frag + ".xml";
			    	Document doc = XMLUtil.parseFile(fragFile);
				    //refs = XMLUtil.xpathNodeSet(doc, "//href[ancestor::xref or ancestor::toolitem]");
				    refs = XMLUtil.xpathNodeSet(doc, "//href[ancestor::xref]");
				    
				    for(int i=0; i<refs.getLength(); i++){
				    	//en-US0900c8af8006ff9e#KC01107005
				    	String ref = ((Element)refs.item(i)).getTextContent();
				    	pl("ref="+ref);
				    	ref = ref.replace("en-US", "");
				    	if(ref.contains("#")){
					    	ref = ref.substring(0, ref.indexOf("#"));
				    	}
				    	ids.add(ref);
				    }
			    }
				insertStmt.setNString(1, frag);
				//not references in this fragment
				if(ids.size()==0){
			    	insertStmt.setNString(2, "NULL");
			    	insertStmt.execute();
				}else{
				    for(int i=0; i<ids.size(); i++){
				    	insertStmt.setNString(2, ids.get(i));
				    	pl("ids.get(i)="+ids.get(i));
				    	insertStmt.execute();
				    }
			    }
			    pl(ids.size() + " records inserted");
		    }
	    }while(frag != null );
	    con.close();
		System.out.println("End of getting all XML Refs, xml fragments count = "+count);
	}

	/*
	static void getAllXMLFragsFromToc() throws Exception{
		System.out.println("Start of getting all XML frags from " + XMLLibDir);
		int count = 0;
		Document doc = XMLUtil.parseFile(tocDir + TOC);
		NodeList lst = doc.getElementsByTagName("NevisId");
		for(int i=0; i<lst.getLength(); i++){
			String fragID = ((Element)lst.item(i)).getAttribute("id") ;
			String fragFile = XMLLibDir + MEPSUtil.getThreeLayerDir(fragID) + fragID + ".xml";
			String copyOfFragFile = inDir + "XML\\" + fragID + ".xml";
			if(!new File(copyOfFragFile).exists()){
				count++;
				FileUtil.copyFile(fragFile, copyOfFragFile);
			}
		}
		System.out.println("End of getting all XML frags, xml files copied = " + count);
	}
	*/
	/**
	 * Turn graphic with OE name into MEPS names
	 */
	static void renameOEs(String srcDir, String destDir) throws Exception{
		FileUtil.flashDir(destDir);
		List<String> files = FileUtil.getAllFilesWithCertainExt(srcDir, "gif");
		System.out.println("Start of renaming "+files.size() + " oenames in folder " + srcDir);
		//Load oe name to MEPS generated_id mappings
		Map<String, String> map = getOE_Generated_id_mapping();
		for(int i=0; i<files.size(); i++){
			String file = files.get(i);
			String oename = file.replace(".gif", "");
			if(file.contains("__")){
				oename = file.substring(0,file.indexOf("__") );
			}
			String gid = map.get(oename);
			(new File(srcDir+file)).renameTo(new File(destDir+ gid.substring(1) + ".gif"));
		}
		System.out.println("End of renaming oenames in folder " + srcDir);
	}
	
	/**
	 * load all volvo codes to PCodes map from table [vv].[dtcCodes]
	 */
	Map<String, String[]> loadCodeMap()throws Exception{
		pl("Loading ebi code map");
		Map<String, String[]> ebiMap = new HashMap<String, String[]>();
		if(isOnline){
			conntect2DB();
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("select ti_id, code, pcode from vv.dtcCodes where model = '" + this.model_fullName + "'");
			//write result to EBI_MAP_FILE
			StringBuilder sb = new StringBuilder();
			while (rs.next()) {
				String ti_id = rs.getString(1).trim() ;
				String code = rs.getString(2).trim() ;
				String pcodesStr = rs.getString(3).trim() ;
				String[] pcodes = pcodesStr.split(",");
				List<String> pcodesLst = new ArrayList<String>();
				for(int i=0; i<pcodes.length; i++){
					if(pcodes[i].trim().length()>2){
						pcodesLst.add(pcodes[i].trim());
					}
				}
				Collections.sort(pcodesLst);
				pcodesStr = "";
				for(String s:pcodesLst){
					pcodesStr += s + ",";
				}
				//if(pcodesStr.endsWith(",")){pcodesStr = pcodesStr.substring(0, pcodesStr.lastIndexOf(","));	}
				
				//the pcodesStr has a tailing comma if it has pcodes in it
				String[] values = {code, pcodesStr};
				ebiMap.put(ti_id, values);
				//pl("ebiMap entry="+ti_id + "  " + code + "  " + pcodesStr );
				
				sb.append(StringEscapeUtils.escapeCsv(ti_id) +"," + StringEscapeUtils.escapeCsv(code) + "," +  
				StringEscapeUtils.escapeCsv(pcodesStr) + System.lineSeparator());
			}
			FileUtil.writer(EBI_MAP_FILE, sb.toString());
		}else{
			String ebiContent = FileUtil.reader(EBI_MAP_FILE);
			if(StringUtils.isNoneBlank(ebiContent)){
				String[] ebiMaps = ebiContent.split(System.lineSeparator());
				for(int i=0; i<ebiMaps.length; i++){
					String[] map = ebiMaps[i].split(",");
					String[] values = {map[1], map[2]};
					pl("read ebiMap:" + Arrays.toString(map));
					ebiMap.put(map[0], values);
				}
			}
		}

			
		return ebiMap;
	}
	
	/**
	 * The input element should be a ti_title <info-obj> element
	 */
	void insertEBI(Map<String, String[]> map, Element ele)throws Exception{
		String id = ele.getAttribute("id");
		String[] values = map.get(id);
		
		String codeName = values[1] + values[0];
		//code.type attribute should already been set, let's set it again
		ele.setAttribute("code.type", "title");
		ele.setAttribute("code.name", codeName);
		NodeList lst = XMLUtil.xpathNodeSet(ele, "/info-obj");
		for(int i=0; i<lst.getLength(); i++){
			Element child = (Element)lst.item(i);
			String codeType ;
			String title = XMLUtil.xpathStr(child, "/title/text()");
			if(title.contains("Diagnostic trouble code")){
				codeType = "cirdesc";
			}else{
				codeType = "test";
			}
			ele.setAttribute("code.type", codeType);
			ele.setAttribute("code.name", codeName);
		}
	}

	
	/**
	 * solve all internal and external references and import necessary orphan fragments
	 * it also insert a comment with original ids for each object 
	 */
	private void solveRefs() throws Exception {
		System.out.println("Start of solving references - " + new Date());
		int count = 0;
		Document doc;
		List<String> files = FileUtil.getAllFilesWithCertainExt(tranformedANDInsertedGXInfoDir, "xml");
		String[] values;
		String id, refFile, type, title;
		Map<String, String[]> ebiMap = loadCodeMap();
		/*
		 * STEP 1: 1.collect all references info 
		 */
		System.out.println(" Collecting all ids - " + new Date());
		for (String file : files) {
			pl("Collecting all ids of file "+file);
			doc = XMLUtil.parseFile(tranformedANDInsertedGXInfoDir + file);
			
			//DTC Index articles should not have referenced IDs (only dummy ids), let's dudup its ids
			if(file.contains("DTCIndex")){
				NodeList nodes = XMLUtil.xpathNodeSet(doc, "//*[@id]");
				for(int i=0; i<nodes.getLength(); i++){
					Element n = (Element)nodes.item(i);
					n.setAttribute("id", "dtcID-"+i);
				}
				FileUtil.writer(refSolvedDir + file, XMLUtil.xmlToString(doc));
				//FileUtil.copyFile(tranformedANDInsertedGXInfoDir + file, refSolvedDir + file);
				continue;
			}
			//need to process each toc fragment notes separately in order to make sure test procedure are self-contained
			NodeList fragNodes = XMLUtil.xpathNodeSet(doc, "/som1/info-obj/info-obj", "/som1/info-obj/para");
			for (int ii = 0; ii < fragNodes.getLength(); ii++) {
				NodeList nodes = XMLUtil.xpathNodeSet(fragNodes.item(ii), "/..",  "", "//*[@id]");
				for (int i = 0; i < nodes.getLength(); i++) {
					count++;
					Element node = (Element) nodes.item(i);
					/*the <para> has no id
					 * 	<info-obj code.type="title" id="TITitle-27776-2115">
						<title>ABS-212 RIGHT FRONT WHEEL SENSOR SIGNAL. INCORRECT WHEEL SPEED (1998)</title>
						<para>
							<ptxt>
						<xref dupFrag="true" refid="en-US0900c8af80425eab-KC01458320"/>
						</ptxt>
					</para>*/
					if(!node.hasAttribute("id")){
						continue;
					}
					id = node.getAttribute("id");

					if(id.equals("en-US0900c8af852f226d-nev15481735n1")){
						pl("node id="+id);
					}
					//already processed
					if(id.startsWith("TITitle-") && refMap.containsKey(id)){
						continue;
					}
					//if(id.contains("0900c8af8047deff")){pl("file="+file+" id="+id);	}
					values = new String[7];
					values[0] = file;
					String nodeName = node.getNodeName();
					values[1] = nodeName;
					values[3] = id;
					//id is too long, replace it with a new id
					//if(id.length()> 32 ){
					if(!id.startsWith("TITitle-")){
						values[3] = "shortID-"+count;
						node.setAttribute("id", values[3]);
						Node comm = doc.createComment("org id = "+id+" ");
						node.insertBefore(comm, node.getFirstChild());
					}
					 
					if(nodeName.equalsIgnoreCase("info-obj")){
						values[2] = XMLUtil.xpathStr(node, "/title/text()");
						if(node.hasAttribute("code.type")){
							
							insertEBI(ebiMap, node);
							
						}
						if(node.hasAttribute("procstep")){
							//pl("process procstep info-obj id="+id);
							int infoObjLayer = XMLUtil.xpathNodeSet(node, "/ancestor::info-obj").getLength();
							//it is deeper than 5th layer of <info-obj>
							if(infoObjLayer >= 5){

								String procstepTitle = XMLUtil.xpathStr(node, "/title/text()");
								String parentInfoObjTitle = XMLUtil.xpathStr(node, "/ancestor::info-obj[1]/title/text()");
								values[2] = parentInfoObjTitle + " => " + procstepTitle;
								values[3] = XMLUtil.xpathStr(node, "/ancestor::info-obj[1]/@id");
								
								//move all its content out, then remove this 6th (or deeper) <info-obj>
								if(!procstepTitle.equals("NO TITLE")){
									Element para = doc.createElement("para");
									Element ptxt = doc.createElement("ptxt");
									Element emph = doc.createElement("emph");
									emph.setAttribute("etype", "bold");
									emph.setTextContent(procstepTitle);
									ptxt.appendChild(emph);
									para.appendChild(ptxt);
									node.getParentNode().insertBefore(para, node);
								}
								
								NodeList lst = XMLUtil.xpathNodeSet(node, "/*[name()!='title']");
								for(int n=0; n<lst.getLength(); n++){
									node.getParentNode().insertBefore(lst.item(n), node);
								}
								XMLUtil.xpathCommentOut(node, "");
							}else{
								node.removeAttribute("procstep");
							}
						}
					}else if (nodeName.equalsIgnoreCase("table")){
						String tableTitle = XMLUtil.xpathStr(node, "/title/text()");
						values[2] = tableTitle;
						//its parent <info-obj> id
						values[4] = XMLUtil.xpathStr(node, "/ancestor::info-obj[1]/@id");
						values[5] = XMLUtil.xpathStr(node, "/ancestor::info-obj[1]/title/text()");
					}else if (nodeName.equalsIgnoreCase("lst-itm")){
						values[2] = XMLUtil.xpathStr(node, "/ptxt/text()");
					}else{
						el("unexpected element name found "+nodeName); 
						//throw new Exception("unexpected element name found "+nodeName);	//TODO resume me				
					}
					refMap.put(id, values);	
					if(node.hasAttribute("IE-ID")){
						refMap.put(node.getAttribute("IE-ID"), values);
					}
					//pl("id added to refMap id="+id+" newID="+values[3]);
				}
				
				//these <intxref> are supposed to be part of test procedures, we need make sure they keep as internal references
				NodeList intxrefs = XMLUtil.xpathNodeSet(fragNodes.item(ii), "//intxref");
				for(int k=0; k<intxrefs.getLength(); k++){
					Element xref = (Element)intxrefs.item(k);
					values = refMap.get(xref.getAttribute("refid"));
					if(values==null || !file.equals(values[0])){
						System.err.println("intxref is pointing to a different file refid="+xref.getAttribute("refid")+ "  file="+file);
						XMLUtil.xpathCommentOut("Borken link ",xref);
						continue;
						//throw new Exception("intxref is pointing to a different file values[0]="+values[0]+ "  file="+file);
					}
					xref.setTextContent(values[2]);
					xref.setAttribute("dest", "info-obj");
					xref.setAttribute("refid", values[3]);
					addReferTo(xref);
				}
			}

			FileUtil.writer(refSolvedDir + file, XMLUtil.xmlToString(doc));
		}
		//put a dummy DTC id to streamline the following statements
		refMap.put("DTCID", new String[7]);
		System.out.println(" " + count + " ids collectd - " + new Date());
		
//refMap.forEach((key, value) -> pl("\n refMap.key="+key+", value=" + Arrays.toString(value)));
		
		// STEP 2: solve all references
		System.out.println(" Solving all references - " + new Date());
		count = 0;
		for (String file : files) {
			pl("Solving references for file " + file);
			doc = XMLUtil.parseFile(refSolvedDir + file);
			NodeList xrefs = XMLUtil.xpathNodeSet(doc, "//xref");
			for (int i = 0; i < xrefs.getLength(); i++) {
				count++;
				Element xref = (Element) xrefs.item(i);
				id = xref.getAttribute("refid");  
				pl("solving refid=" + id);
				if(id.length() < 11){
					el("Ignoring invalid reference:" + id);
					continue;
				}
				if(!refMap.containsKey(id)){
					if(!id.startsWith("TITitle") && 
							(isNonExistingID(id) ||	
									//these ids pointing to commented-out <testgrp>; the <testgrp> is commented out because it takes you back to the begining of the test procedure
									id.equals("en-US0900c8af8201722d-KC01691044"))){
						System.err.println("non exisitng id in source found="+id + " file="+file);
						xref.setTextContent("non exisitng id in source " + xref.getTextContent());
						XMLUtil.xpathCommentOut(xref, "");
					
					//these ids (e.g. en-US0900c8af80422bee-KC01497754) are commented out <testgrp> with only <diagcallout> in it, 
					//let's redirect the reference to the item referenced by its <diagcallout>
					}else if(id.equalsIgnoreCase("en-US0900c8af80422bee-KC01497754")){
						id = "en-US0900c8af80423198-KC01576491";
					}else if(id.equalsIgnoreCase("en-US0900c8af8201722d-KC01691045")){
						id = "en-US0900c8af82017181-KC01616835";
					}else if(id.equalsIgnoreCase("en-US0900c8af8201722d-KC01691046")){
						id = "en-US0900c8af820171811005-KC01616835";
					}else{
						/*if(1==1 || id.contains("0900c8af8185405a")){
							pl("id not found id="+id + " file="+file);
						}else{
							throw new Exception("id not found id="+id + " file="+file);
						}*/
						//throw new Exception("id not found id="+id + " file="+file);
						el("id not found id="+id + " file="+file);
					}
					continue;
				}
				values = refMap.get(id);
				refFile = values[0];
				type = values[1];
				title = values[2];
				String newID = values[3];
				String tempID = id;
				while(!tempID.equals(newID) && refMap.get(newID)!=null){
					tempID = newID;
					newID = refMap.get(tempID)[3];
				}
				//pl("id==="+id+" type="+type+" file="+refFile);
				
				Element newRef;
				if (!file.contains("DTCIndex") && file.equalsIgnoreCase(refFile)) {
					newRef = doc.createElement("intxref");
					newRef.setAttribute("refid", newID);
					newRef.setTextContent(title);
					if (type.equalsIgnoreCase("info-obj")) {
						newRef.setAttribute("dest", "info-obj");
						//addReferTo(xref);
					} else if(type.equalsIgnoreCase("table")){
						newRef.setAttribute("dest", "tbl");
					}else if(type.equalsIgnoreCase("lst-itm")){
						newRef.setAttribute("dest", "lst-itm");
					}else{
						throw new Exception("unexpected element type found1 "+type);
					}
				} else {
					newRef = doc.createElement("extxref");
					newRef.setAttribute("filetype", "SGML");
					newRef.setAttribute("extrefid", newID);
					newRef.setAttribute("document",	refFile.replace(".xml", ""));
					if (file.contains("DTCIndex")) {
						String textContent = sortCommaDelimitedString(xref.getTextContent().trim()).replace(",", "\n");
						newRef.setTextContent(textContent);
					} else {
						if (type.equalsIgnoreCase("info-obj")) {
							newRef.setTextContent(title);
							//addReferTo(xref);
						} else if(type.equalsIgnoreCase("table")){
							//redirect to the <table>'s parent <info-obj>
							newRef.setAttribute("extrefid", values[4]);
							newRef.setTextContent(values[5] + " => Table:"+title);
							pl("external table reference found:"+"id==="+id+" type="+type+" file="+refFile+" values[4]="+values[4]+" new Title="+values[5] + " => Table:"+title );
							//addReferTo(xref);
						}else{
							pl("external reference found:"+"id==="+id+" type="+type+" file="+refFile);
							throw new Exception("unexpected element type found2 "+type);
						}
					}
				}
				boolean isDupFragment = xref.hasAttribute("dupFrag");
				xref.getParentNode().insertBefore(newRef, xref);	
				xref.getParentNode().removeChild(xref);
				
				//add a space between newRef and its preceding test if applicable
				addSpace(newRef);
				/*if(newRef.getParentNode().getNodeName().equalsIgnoreCase("ptxt")){
					Node precedingText = XMLUtil.xpathNode(newRef, "/preceding-sibling::text()[1]");
					if(precedingText!=null){
						String text = precedingText.getTextContent();
						if(!text.endsWith(" ") && !text.endsWith(":")){
							precedingText.setTextContent(text+" ");
						}
					}
				}*/
				
				//if this <xref> is duplicate XML fragment reference, wrap it with <info-obj> to comply with the DTD
				if(isDupFragment){
					//<para><ptxt><intxref refid="en-US0900c8af80c4bcb9" dest="info-obj"></intxref></ptxt></para>
					Element para = (Element)XMLUtil.xpathNode(newRef, "/ancestor::para");
					Element infoObjWrapper = doc.createElement("info-obj");
					Element infoObjWrapperTitle = doc.createElement("title");
					infoObjWrapperTitle.setTextContent(title);
					
					infoObjWrapper.setAttribute("id", "infoObjWrapper-"+count);
					infoObjWrapper.appendChild(infoObjWrapperTitle);
					para.getParentNode().insertBefore(infoObjWrapper, para);
					infoObjWrapper.appendChild(para);
				}
			}
			XMLUtil.xpathRemoveAtt(doc, "IE-ID", "//info-obj");
			FileUtil.writer(refSolvedDir + file, XMLUtil.xmlToString(doc));
			System.out.println(" " + xrefs.getLength() + " references solved in file " + file);
		}
		
		System.out.println("End of solving references, references soloved="	+ count);
	}
	
	/**
	 *Try to eliminate no title <info-obj>
	 *given a NO TITLE <info-obj>, we wonâ€™t touch it unless its parent <info-obj> has one and only one child <info-obj>  
	 *Seems not of these eliminated no title <info-obj> is referenced!!!!
	 */
	void eliminateNoTitleInfoObj()throws Exception{
		System.out.println("Start Eliminating no title <info-obj>");
		//reuse refMap
		refMap.clear();
		List<String> files = FileUtil.getAllFilesWithCertainExt(refSolvedDir, "xml");
		Document doc;
		int co = 0;
		for (String file : files) {
			doc = XMLUtil.parseFile(refSolvedDir + file);
			//eliminateNoTitleInfoObj(doc, file);
			//pl("3doc="+XMLUtil.xmlToStringNoHeader(doc));
			NodeList lst = XMLUtil.xpathNodeSet(doc, "/som1/info-obj//info-obj[count(info-obj)=1 and info-obj/title/text()='NO TITLE']");
			pl("number of no title <info-obj> will be removed="+lst.getLength()+ " in file="+file);
			for(int i=0; i<lst.getLength(); i++){
				Element ele = (Element)lst.item(i);
				Element noTitleInfoObj =  (Element)XMLUtil.xpathNode(ele, "/info-obj");
				noTitleInfoObj.setAttribute("removeme", "true");
				String id = noTitleInfoObj.getAttribute("id");
				String[] values;
				//this id has been assigned a new id (probably shortID-XX)
				if(refMap.get(id)!=null){
					throw new Exception("dup id found id="+id);
				}
				values = new String[4];
				//file and elementType should not change
				//values[0] = file;
				//values[1] = "info-obj";
				values[2] = XMLUtil.xpathStr(ele, "/title/text()");
				values[3] = ele.getAttribute("id");
				refMap.put(id, values);
			}
			
			while(XMLUtil.xpathNodeSet(doc, "//info-obj[@removeme]").getLength()>0){
				//start with leaf <info-obj>
				lst = XMLUtil.xpathNodeSet(doc, "//info-obj[@removeme and not(descendant::info-obj[@removeme])]");
				for(int i=0; i<lst.getLength(); i++){
					Element infoObj = (Element)lst.item(i);
					NodeList nodes = XMLUtil.xpathNodeSet(infoObj, "/*[name()!='title']");
					for (int j = 0; j < nodes.getLength(); j++) {
						infoObj.getParentNode().insertBefore(nodes.item(j), infoObj);
					}
					XMLUtil.xpathCommentOut(infoObj, "");
				}
			}
			co += lst.getLength();
			FileUtil.writer(refSolvedDir + file, XMLUtil.xmlToString(doc));
		}
		pl("Totoal number of no title <info-obj> removed="+co);
		
		for (String file : files) {
			co=0;
			doc = XMLUtil.parseFile(refSolvedDir + file);
			NodeList lst = XMLUtil.xpathNodeSet(doc, "//intxref", "//extxref");
			for(int i=0; i<lst.getLength(); i++){
				Element xref = (Element)lst.item(i);
				String id;
				if(xref.getNodeName().equals("intxref")){
					id = xref.getAttribute("refid");
				}else{
					id = xref.getAttribute("extrefid");
				}
				String newID = id;
				String title = "";
				while(refMap.get(newID) != null){
					title = refMap.get(newID)[2];
					newID = refMap.get(newID)[3];
				}
				//id changed
				if(!id.equals(newID)){
					co++;
					if(xref.getNodeName().equals("intxref")){
						xref.setAttribute("refid", newID);
					}else{
						xref.setAttribute("extrefid", newID);
					}
					xref.setTextContent(title);
					//pl("id updated from id="+id + " to newID="+newID+" in file="+file);
				}
			}
			System.out.println("reslove number of xrefs="+co+" in file "+file);
			FileUtil.writer(refSolvedDir + file, XMLUtil.xmlToString(doc));
		}
		System.out.println("End Eliminating no title <info-obj>");
	}
	
	/**
	 *Some orphans are not referenced, thus can be removed
	 */
	int removeUnusedOrphans()throws Exception{
		System.out.println("Start Removing unreferenced orphans");
		//reuse refMap
		refMap.clear();
		List<String> files = FileUtil.getAllFilesWithCertainExt(refSolvedDir, "xml");
		Document doc;
		int co = 0;
		boolean isCollected = false;
		if(refMapTemp.size()>0){
			isCollected = true;
		}
		//collect all related references first
		for (String file : files) {
			if(file.contains("DTCIndex")){
				continue;
			}
			
			//regular articles are already collected and not need to collect again
			if(isCollected){
				if(!file.contains("-Orp-")){
					continue;
				}
			}
			
			doc = XMLUtil.parseFile(refSolvedDir + file);
			
			NodeList lst = XMLUtil.xpathNodeSet(doc, "//extxref[contains(@document, '-Orp-')]");
			for(int i=0; i<lst.getLength(); i++){
				Element ele = (Element)lst.item(i);
				String id = ele.getAttribute("extrefid");
				String document = ele.getAttribute("document");
				String[] values = {document};
				//this should never happen
				if(refMap.containsKey(id)){
					if(!refMap.get(id)[0].equalsIgnoreCase(document)){
						throw new Exception("document don't match! id="+id+" refMap.get(id)[0]="+refMap.get(id)[0]+" document="+document);
					}
				}else{
					if(file.contains("-Orp-")){
						refMap.put(id,values);
					}else{
						refMapTemp.put(id,values);
					}
				}
			}
			
			if(file.contains("-Orp-")){
				lst = XMLUtil.xpathNodeSet(doc, "//intxref");
				for(int i=0; i<lst.getLength(); i++){
					Element ele = (Element)lst.item(i);
					String id = ele.getAttribute("refid");
					String[] values = {file.replace(".xml", "")};
					Element parentFrag = (Element)XMLUtil.xpathNode(ele, "/ancestor::info-obj[count(ancestor::info-obj)=1]");
					Node referencedInfoObj = XMLUtil.xpathNode(parentFrag, "/descendant-or-self::*[@id='"+id+"']");
					//it is not reference to a <info-obj> within the same fragment
					if(referencedInfoObj==null){
						//this should never happen
						if(refMap.containsKey(id)){
							if(!refMap.get(id)[0].equalsIgnoreCase(file.replace(".xml", ""))){
								throw new Exception("document don't match! id="+id+" refMap.get(id)[0]="+refMap.get(id)[0]+" file="+file);
							}
						}else{
							refMap.put(id,values);
						}
					}
				}
			}
		}
		
		refMap.putAll(refMapTemp);
		
		pl("End of collecting ids; number of ids collected="+refMap.size());
		
		//remove unreferenced fragment
		files = FileUtil.getAllFilesWithCertainExt(refSolvedDir, "xml");
		for (String file : files) {
			if(!file.contains("-Orp-")){
				continue;
			}
			doc = XMLUtil.parseFile(refSolvedDir + file);
			NodeList frags = XMLUtil.xpathNodeSet(doc, "/som1/info-obj/info-obj");
			for(int j=0; j<frags.getLength(); j++){
				Element frag = (Element)frags.item(j);
				NodeList lst = XMLUtil.xpathNodeSet(frag, "/descendant-or-self::*[@id]");
				boolean referenced = false;
				for(int i=0; i<lst.getLength(); i++){
					Element ele = (Element)lst.item(i);
					String id = ele.getAttribute("id");
					if(refMap.containsKey(id)){
						referenced = true;
						continue;
					}
				}
				if(!referenced){
					co++;
					//pl("one orphan fragment removed id="+frag.getAttribute("id")+" file="+file);
					XMLUtil.xpathRemove(frag, "/node()");
					XMLUtil.xpathCommentOut(frag, "");
				}
			}
			if(XMLUtil.xpathNodeSet(doc, "/som1/info-obj/info-obj").getLength()==0){
				new File(refSolvedDir + file).delete();
			}else{
				FileUtil.writer(refSolvedDir + file, XMLUtil.xmlToString(doc));
			}
		}
		System.out.println("End Removing unreferenced orphans, orphans removed="+co);
		return co;
	}
	
	
	void countNoTitleInfoObj()throws Exception{
		List<String> files = FileUtil.getAllFilesWithCertainExt(refSolvedDir, "xml");
		Document doc; int count =0;
		for (String file : files) {
			doc = XMLUtil.parseFile(refSolvedDir + file);
			String co = XMLUtil.xpathStr(doc, "/count(//info-obj[title/text()='NO TITLE'])");
			if(!co.equals("0")){
				count += Integer.parseInt(co);
				System.out.println("NO TITLE info obj count - file="+file+" count="+co);
			}
		}
		System.out.println("Total "+count);
	}

	/*void eliminateNoTitleInfoObj(Document doc, String file)throws Exception{
		NodeList nodes = XMLUtil.xpathNodeSet(doc , "/som1/info-obj/info-obj/info-obj");
		for (int i = 0; i < nodes.getLength(); i++) {
			eliminateNoTitleInfoObj((Element)nodes.item(i), file);
		}
	}
	
	void eliminateNoTitleInfoObj(Element infoObj, String file)throws Exception{
		if(infoObj.getElementsByTagName("info-obj").getLength() == 0){
			return;
		}
		boolean eliminated = false;
		Element parent = (Element)infoObj.getParentNode();
		String title = XMLUtil.xpathStr(infoObj, "/title[1]/text()");
		if(title.equals("NO TITLE")){
			boolean isFirstChild = XMLUtil.xpathNodeSet(infoObj, "/preceding-sibling::info-obj").getLength() == 0;
			boolean noChildrenOtherThanInfoObj = false;
			if(!isFirstChild){
				noChildrenOtherThanInfoObj = XMLUtil.xpathNodeSet(infoObj, "/*[name()!='title' and name()!='info-obj']").getLength() == 0;
			}
			//this <info-obj> can be elimated
			if(isFirstChild || noChildrenOtherThanInfoObj){
				eliminated = true;
				String id = infoObj.getAttribute("id");
				String firstChildInfoObjID = XMLUtil.xpathStr(infoObj, "/info-obj[1]/@id");
				String firstChildInfoObjTitle = XMLUtil.xpathStr(infoObj, "/info-obj[1]/title/text()");
				NodeList nodes = XMLUtil.xpathNodeSet(infoObj, "/*[name()!='title']");
				for (int i = 0; i < nodes.getLength(); i++) {
					parent.insertBefore(nodes.item(i), infoObj);
				}
				XMLUtil.xpathCommentOut(infoObj, "");
				//pl("eliminateNoTitleInfoObj id ="+id);
				String[] values;
				//this id has been assigned a new id (probably shortID-XX)
				if(refMap.get(id)==null){
					values = new String[4];
					//file and elementType should not change
					//values[0] = file;
					//values[1] = "info-obj";
					values[2] = firstChildInfoObjTitle;
					values[3] = firstChildInfoObjID;
					refMap.put(id, values);
				}else{
					throw new Exception("duplicate ids found id="+id);
				}
				pl("NO Title info-obj eliminated; id="+id+ " firstChildInfoObjID="+firstChildInfoObjID);
			}
		}
		
		NodeList nodes ;
		if(eliminated){
			nodes = XMLUtil.xpathNodeSet(parent , "/info-obj");
		}else{
			nodes = XMLUtil.xpathNodeSet(infoObj , "/info-obj");
		}
		for (int i = 0; i < nodes.getLength(); i++) {
			eliminateNoTitleInfoObj((Element)nodes.item(i), file);
		}
	}*/
	
	//check if given id is a non-existing id in Volvo source
	static boolean isNonExistingID(final String inputID) {
		//en-US0900c8af844cd4f2   or    en-US0900c8af844cd4f2-nev13713880n99 or en-US0900c8af84fdc2c2-nev20688606n1-nev15841649n244
		//known non-existing documents  
		if(inputID.startsWith("en-US0900c8af80ce0712") || inputID.startsWith("en-US0900c8af83b4bcee")
				|| inputID.startsWith("en-US0900c8af80cc61f2")){
			return true;
		}
		pl("check id existence id="+inputID);
		String id = inputID.replace("en-US", "");
		try{
			if(id.contains(IDJOIN)){ //en-US0900c8af844cd4f2-nev13713880n99, en-US0900c8af84fdc2c2-nev20688606n1-nev15841649n244
				String fragID = id.substring(0, id.indexOf(IDJOIN));
				Document doc = getRawXMLFragments(fragID);
				
				String srcID = id.substring(id.lastIndexOf(IDJOIN) + IDJOIN.length());
				//pl("doc="+XMLUtil.xmlToStringNoHeader(doc));
				NodeList targetNodes = XMLUtil.xpathNodeSet(doc, "//*[@id='"+srcID+"']");
				//pl("fragID="+fragID+" srcID="+srcID);
				return(targetNodes.getLength() == 0);
			}else{//en-US0900c8af844cd4f2
				Document doc = getRawXMLFragments(id);
				return doc == null;
			}
		}catch(Exception e){
			el("Not existing document:"+inputID);
			return true;
		}


	}
	
	/*private static void solveRefs() throws Exception {
		System.out.println("Start of solving references - " + new Date());
		int count = 0;
		Document doc;
		List<String> files = FileUtil.getAllFilesWithCertainExt(
				tranformedANDInsertedGXInfoDir, "xml");
		String[] values;
		String id, refFile, type, title;
		System.out.println(" Collecting all ids - " + new Date());
		for (String file : files) {
			pl("Collecting xrefs of file "+file);
			doc = XMLUtil.parseFile(tranformedANDInsertedGXInfoDir + file);
			// RM000000U9U06VX@@@PRECAUTION
			NodeList nodes = XMLUtil.xpathNodeSet(doc,
							"//info-obj[not(contains(@id, 'dummy'))] | //lst-itm[@id and not(contains(@id, 'dummy'))]");
			for (int i = 0; i < nodes.getLength(); i++) {
				count++;
				Element node = (Element) nodes.item(i);
				id = node.getAttribute("id");

				// 2.insert each <info-obj> or <lst-itm> with a comment containing original id
				Node idComment = doc.createComment(id.replace("-", "_"));
				node.insertBefore(idComment, node.getFirstChild());

				values = new String[3];
				values[0] = file;
				// System.out.println("infoObjInfo.item(i)"+XMLUtil.xmlToString(infoObjInfo.item(i)));
				if (node.getNodeName().equalsIgnoreCase("info-obj")) {
					values[1] = "info-obj";
					title = XMLUtil.getDirectChildElementsByTagName(node, "title")
							.get(0).getTextContent();
				} else { // <lst-itm>
					String originalTag = node.getAttribute("originalTag");
					// if it has one and only one <ptxt> child
					//String onePtxtOnly = XMLUtil.xpathStr(node,	"/(ptxt and count(*)=1)");
					//int xrefs = XMLUtil.xpathNodeSet(node, "//xref").getLength();
					if (originalTag.equals("testgroup")	//&& onePtxtOnly.equalsIgnoreCase("true") && xrefs == 0
							) {
						values[1] = "testgroup";
						title = XMLUtil.xpathStr(node, "/ptxt/emph/text()");
						node.getParentNode().removeChild(node);
					} else {
						values[1] = "lst-itm";
						title = XMLUtil.getXpathPredicate(node) + "";
					}
				}
				values[2] = title;
				if (refMap.containsKey(id)) {
					String dupFileName = refMap.get(id)[0];
					throw new Exception("Unexpeced duplicate ID found; id="
							+ id + " file=" + file + " dupFile=" + dupFileName);
				} else {
					// System.out.println(id + " " + values[0] + " "+values[1] +
					// "  "+ values[2]);
					refMap.put(id, values);
				}
			}
			FileUtil.writer(refSolvedDir + file, XMLUtil.xmlToString(doc));
		}
		System.out.println(" " + count + " ids collectd - " + new Date());
		// STEP 2: solve all references
		System.out.println(" Solving all references - " + new Date());
		count = 0;
		for (String file : files) {
			doc = XMLUtil.parseFile(refSolvedDir + file);
			// <xref be_see_page="Click here for more information."
			// be_see_page_prefix="See Page" be_see_page_suffix="."
			// linkend="RM000000U3S01BX_01_0009"/>
			NodeList xrefs = XMLUtil.xpathNodeSet(doc, "//xref");
			for (int i = 0; i < xrefs.getLength(); i++) {
				count++;
				Element xref = (Element) xrefs.item(i);
				Node parent = xref.getParentNode();
				// RM000000U3S01BX_01_0009
				id = xref.getAttribute("linkend").replace("_", "-");
//pl("id="+id);
				values = refMap.get(id);
				refFile = values[0];
				type = values[1];
				title = values[2];
				Element newRef;
				if (type.equalsIgnoreCase("testgroup")) {
					Node text = doc.createTextNode(" " + title);
					parent.insertBefore(text, xref);
					parent.removeChild(xref);
				} else {
					if (file.equalsIgnoreCase(refFile)) {
						newRef = doc.createElement("intxref");
						newRef.setAttribute("refid", id);
						if (type.equalsIgnoreCase("info-obj")) {
							newRef.setAttribute("dest", "info-obj");
							//Node text = doc.createTextNode(". Refer to ");
							//parent.insertBefore(text, xref);
							addReferTo(xref);
							newRef.setTextContent(title);
						} else {
							newRef.setAttribute("dest", "lst-itm");
							Node text = doc.createTextNode(" See step ");
							parent.insertBefore(text, xref);
							//newRef.setTextContent(". See step "+title);
						}
					} else {
						// external reference (<extxref document="08GS350_01-00"
						// filetype="SGML" extrefid="S22">Diagnostics</extxref>)
						newRef = doc.createElement("extxref");
						newRef.setAttribute("document",	refFile.replace(".xml", ""));
						newRef.setAttribute("filetype", "SGML");
						newRef.setAttribute("extrefid", id);
						if (file.contains("DTCIndex")) {
							Node parentEntry = XMLUtil.xpathNode(xref,
									"/ancestor::entry");
							String dtcCode = XMLUtil.xpathStr(parentEntry,
									"/ptxt[1]/text()[1]");
							XMLUtil.xpathRemove(parentEntry, "/ptxt[1]");
							newRef.setTextContent(dtcCode);
						} else {
							if (type.equalsIgnoreCase("info-obj")) {
								//Node text = doc.createTextNode("Refer to ");
								//parent.insertBefore(text, xref);
								addReferTo(xref);
								newRef.setTextContent(title);
							} else { // must be <lst-itm>
										// find its parent <info-obj> title
								String parentTitle;
								String parentID = id.substring(0,id.lastIndexOf("-"));
								if (refMap.containsKey(parentID)) {
									parentTitle = refMap.get(parentID)[2];
									newRef.setAttribute("extrefid", parentID);
								} else { // should never occur
									throw new Exception(
											"lst-itm id's parent id is not an info-obj!!! file="
													+ file + " id=" + id);
									// parentID = parentID.substring(0,
									// id.lastIndexOf("-"));
									// parentTitle = refMap.get(parentID)[2];
								}
								//Node text = doc.createTextNode(". Refer to ");
								//parent.insertBefore(text, xref);
								addReferTo(xref);
								newRef.setTextContent(parentTitle + " - Step " + title);
							}
						}
					}
					parent.insertBefore(newRef, xref);
					parent.removeChild(xref);
				}
			}
			FileUtil.writer(refSolvedDir + file, XMLUtil.xmlToString(doc));
			System.out.println(" " + xrefs.getLength() + " references solved in file " + file);
		}
		System.out.println("End of solving references, references soloved="	+ count);
	}*/
	
	static void addSpace(Node xref){
		String refTo = null;
		Node preSibling = xref.getPreviousSibling();
		if(preSibling != null){
			if(preSibling.getNodeType() == Node.TEXT_NODE && !preSibling.getTextContent().endsWith(" ") && !preSibling.getTextContent().endsWith(":")){
				refTo = " ";
			}
			if(preSibling.getNodeName().equalsIgnoreCase("emph") && !preSibling.getTextContent().endsWith(" ") && !preSibling.getTextContent().endsWith(":")){
				refTo = " ";
			}
		}
		if(refTo!=null){
			Node text = xref.getOwnerDocument().createTextNode(refTo);
			xref.getParentNode().insertBefore(text, xref);
		}
	}
	
	static void addReferTo(Node xref){
		String refTo = "Refer to ";
		Node preSibling = xref.getPreviousSibling();
		if(preSibling != null){
			if(preSibling.getNodeType() == Node.TEXT_NODE && preSibling.getTextContent().length()>0){
				refTo = ". " + refTo;
			}
			if(preSibling.getNodeName().equalsIgnoreCase("emph") && preSibling.getTextContent().length()>0){
				refTo = ". " + refTo;
			}
		}
		Node text = xref.getOwnerDocument().createTextNode(refTo);
		xref.getParentNode().insertBefore(text, xref);
	}

	
	/**
	 * convert final XML to SOM1 SGML format
	 */
	void toSGML() throws Exception{
		List<String> files = FileUtil.getAllFilesWithCertainExt(refSolvedDir , "xml");
		for(int i =0; i<files.size(); i++){
			System.out.println("Converting file "+files.get(i) + " to SGML");
			Document doc = XMLUtil.parseFile(refSolvedDir + files.get(i));
			
			//move explanation marks
			NodeList nodes = XMLUtil.xpathNodeSet(doc, "//ptxt[ancestor::warning]","//ptxt[ancestor::caution]", "//ptxt[ancestor::note]", "//ptxt[ancestor::important]");
			for(int j=0; j<nodes.getLength(); j++){
				Element ptxt = (Element)nodes.item(j);
				NodeList texts = XMLUtil.xpathNodeSet(ptxt, "/text()");
				for(int k=0; k<texts.getLength(); k++){
					Node text = texts.item(k);
					text.setTextContent(text.getTextContent().replace("!", ""));
				}
			}
			
			/*int numberOfInfoObj = XMLUtil.xpathNodeSet(doc, "//info-obj[title/text()='NO TITLE']").getLength();
			if(numberOfInfoObj>0){
				pl("NO TITLE <info-obj> found in file="+files.get(i)+ " count="+ numberOfInfoObj);
			}*/
			
			//do a final checks before converting it to SGML
			MEPSUtil.finalCheck(doc, files.get(i));
			XMLUtil.validAgainstSOM1Schema(doc);
			
			String content = XMLUtil.xml2Article(doc);
			//content = XMLUtil.removeNonUtf8CompliantCharacters(content);
			content = MEPSUtil.charMapping(content);
			content = content.replace("@AnD@", "&");
			content = content.replace("INFORMATIONECM-", "INFORMATION ECM-");
			content = content.replace("[circlef]","&circlef; ");
			
			
			FileUtil.writer(ModelOutDir + files.get(i).replace("xml", "sgm"), content);
		}
	}
	
	/**
	 * insert torque info
	 */
	static void inserttorque(Document doc) throws Exception{
		System.out.println("Start of inserting torque");
		NodeList lst = XMLUtil.xpathNodeSet(doc, "//*[torque]");
		for(int i=0; i<lst.getLength(); i++){
			Element torqueParent = (Element)lst.item(i);
			
			if(!torqueParent.getNodeName().equals("ptxt")){
				throw new Exception("torque not in <ptxt>; " + XMLUtil.xmlToStringNoHeader(torqueParent));
			}
			
			if(XMLUtil.xpathNodeSet(torqueParent, "/*[name()!='torque']").getLength()>0){
				System.err.println("torque parent <ptxt> has non <torque> child; "+XMLUtil.xmlToStringNoHeader(torqueParent));
				//throw new Exception("torque parent <ptxt> has more than one child; "+XMLUtil.xmlToStringNoHeader(parentNode));
			}
			
			NodeList torques = XMLUtil.xpathNodeSet(torqueParent, "/torque");
			for(int j=0;j<torques.getLength(); j++){
				Element torque = (Element)torques.item(j);
				String fragId = torque.getAttribute("refid");
				
				//if(fragId.endsWith("-")) fragId = fragId.substring(0, fragId.length()-1);
				if(StringUtils.isBlank(fragId)){
					el("empty torque refid found:" + XMLUtil.xmlToStringNoHeader(torque));
					continue;
				}else{
					pl(" good torque refid found:" + XMLUtil.xmlToStringNoHeader(torque));
				}
					
				
				Document fragDoc = getXMLFragments(fragId, null);
				Element torqueInfo = (Element)doc.importNode(fragDoc.getDocumentElement(), true);
				torqueParent.insertBefore(torqueInfo, torque);
				XMLUtil.xpathCommentOut(torque);
			}
		}
		System.out.println("End of inserting torque");
	}
	
	/*static void inserttorque(Document doc) throws Exception{
		//fragCache
		System.out.println("Start of inserting torque");
		NodeList lst = XMLUtil.xpathNodeSet(doc, "//torque");
		for(int i=0; i<lst.getLength(); i++){
			Element torque = (Element)lst.item(i);
			Element parentNode = (Element)torque.getParentNode();
			
			if(!parentNode.getNodeName().equals("ptxt")){
				throw new Exception("torque not in <ptxt>; torque id="+torque.getAttribute("id"));
			}
			if(XMLUtil.xpathNodeSet(parentNode, "/*[name()!='torque']").getLength()>0){
				System.err.println("torque parent <ptxt> has non <torque> child; "+XMLUtil.xmlToStringNoHeader(parentNode));
				//throw new Exception("torque parent <ptxt> has more than one child; "+XMLUtil.xmlToStringNoHeader(parentNode));
			}
			
			String fragId = torque.getAttribute("refid");
			
			if(fragId.endsWith("-")) fragId = fragId.substring(0, fragId.length()-1);//
			
			
			Document fragDoc = getXMLFragments(fragId, null);
			Element torqueInfo = (Element)doc.importNode(fragDoc.getDocumentElement(), true);
			//replace <torque>, including its parent <ptxt>
			parentNode.getParentNode().insertBefore(torqueInfo, parentNode);
			XMLUtil.xpathCommentOut(parentNode);
		}
		System.out.println("End of inserting torque");
	}*/

	/**
	 *Using XSLT to transform XMLs 
	 */
	static void tranform() throws Exception {
		System.out.println("Start of tranform and insert graphic info from " +  fragInsertedDir);
		List<String> files = FileUtil.getAllFilesWithCertainExt(fragInsertedDir, "xml");
		for (int j = 0; j < files.size(); j++) {
			// String fileName = "09GS350_01-00.xml";
			String fileName = files.get(j);
			System.out.println("Start of tranform file "+fileName + new Date());
			Document doc = XMLUtil.parseFile(fragInsertedDir+fileName);
			
			inserttorque(doc);
			
			String xslID ;
			Document resultDoc ;
			if(fileName.contains("DTCIndex")){
				xslID = scriptDir + "Volvo_DTCIndex.xsl";
				resultDoc = XSLTUtil.transformer(doc, xslID, "BaseID", j+"", "InFname", "DTCIndex");

			}else{
				xslID = scriptDir + "Volvo_base.xsl";
				resultDoc = XSLTUtil.transformer(doc, xslID, "BaseID", j+"");
				
				//pl("XMLUtil.xmlToString(resultDoc)"+XMLUtil.xmlToString(resultDoc));
				resultDoc = XMLUtil.parseStr(XMLUtil.xmlToString(resultDoc).replace("&lt;&lt;&lt;", "<").replace("&gt;&gt;&gt;", ">"));
				
				xslID = Util.TOOLS_DIR + "xslt\\" + "cleanup_List.xsl";
				for (int i = 0; i < 3; i++) {
					resultDoc = XSLTUtil.transformer(resultDoc, xslID);
				}

				xslID = Util.TOOLS_DIR + "xslt\\" + "cleanup_ptxt.xsl";
				resultDoc = XSLTUtil.transformer(resultDoc, xslID);

				xslID = Util.TOOLS_DIR + "xslt\\"  + "cleanup_misc.xsl";
				resultDoc = XSLTUtil.transformer(resultDoc, xslID);
				
				//if(1==1)continue;

				NodeList lst = resultDoc.getElementsByTagName("*");
				for (int i = 0; i < lst.getLength(); i++) {
					Element ele = (Element) lst.item(i);
					ele.removeAttribute("cleaned");
				}
				
				XMLUtil.xpathCommentOut(resultDoc, "//para[not(ptxt)]");
				
				MEPSUtil.capTitles(resultDoc);
				// insertGXInfo(resultDoc);  TODO resume this, graphics inserting is not done yet 1.23.2019
				MEPSUtil.insertTableTitle(resultDoc);
				
				//move elements which have preceding-sibling::info-obj into its precding-sibling <info-obj> elements
				NodeList ns = XMLUtil.xpathNodeSet(resultDoc, "//*[name()!='info-obj' and preceding-sibling::info-obj]");
				while(ns.getLength()>0){
					//pl("info-obj cleanup loop:"+count++);
					for(int i=0; i<ns.getLength(); i++){
						Node precedingInfoObj = XMLUtil.xpathNode(ns.item(i), "/preceding-sibling::info-obj[1]");
						precedingInfoObj.appendChild(ns.item(i));
						//pl("node moved into it preceding <info-obj>; info-obj/@id="+((Element)precedingInfoObj).getAttribute("id")+" nodeName="+ns.item(i).getNodeName());
					}
					ns = XMLUtil.xpathNodeSet(resultDoc, "//*[name()!='info-obj' and preceding-sibling::info-obj]");
				}
			}
			
			MEPSUtil.fixTableColWidth(resultDoc);
			
			FileUtil.writer(tranformedANDInsertedGXInfoDir + fileName, XMLUtil.xmlToString(resultDoc).
					replace("VV_SpecialToolsDocumentHolder", SPECIAL_TOOL_FILE));
		}
		caption_INFO = null;
		OE_INFO = null;
		System.out.println("End of tranform and insert graphic info, number of files processed="+files.size() );
	}
	
	/*static void tranform() throws Exception {
		System.out.println("Start of tranform and insert graphic info from " +  fragInsertedDir);
		List<String> files = FileUtil.getAllFilesWithCertainExt(fragInsertedDir, "xml");
		for (int j = 0; j < files.size(); j++) {
			// String fileName = "09GS350_01-00.xml";
			String fileName = files.get(j);
			System.out.println("Start of tranform file "+fileName + new Date());
			String sourceID = fragInsertedDir + fileName;
			String xslID ;
			String destID = tranformedANDInsertedGXInfoDir + fileName;
			if(fileName.contains("DTCIndex")){
				xslID = scriptDir + "Volvo_DTCIndex.xsl";
				XSLTUtil.transformer(sourceID, xslID, destID, "BaseID", j+"", "InFname", "DTCIndex");
			}else{
				xslID = scriptDir + "Volvo_base.xsl";
				XSLTUtil.transformer(sourceID, xslID, destID, "BaseID", j+"");
				
				sourceID = tranformedANDInsertedGXInfoDir + fileName;
				xslID = Util.TOOLS_DIR + "xslt\\" + "cleanup_List.xsl";
				destID = tranformedANDInsertedGXInfoDir + fileName;
				for (int i = 0; i < 3; i++) {
					XSLTUtil.transformer(sourceID, xslID, destID);
				}

				xslID = Util.TOOLS_DIR + "xslt\\" + "cleanup_ptxt.xsl";
				XSLTUtil.transformer(sourceID, xslID, destID);

				xslID = Util.TOOLS_DIR + "xslt\\"  + "cleanup_misc.xsl";
				XSLTUtil.transformer(sourceID, xslID, destID);
				
				//if(1==1)continue;

				Document docu = XMLUtil.parseFile(destID);
				NodeList lst = docu.getElementsByTagName("*");
				for (int i = 0; i < lst.getLength(); i++) {
					Element ele = (Element) lst.item(i);
					ele.removeAttribute("cleaned");
				}
				//listEnum(docu);
				
				MEPSUtil.capTitles(docu);
				insertGXInfo(docu);
				MEPSUtil.insertTableTitle(docu);
				inserttorque(docu);
				
				//insertTITitle(docu);
				
				FileUtil.writer(destID, XMLUtil.xmlToString(docu).replace("VV_SpecialToolsDocumentHolder", SPECIAL_TOOL_FILE));
			}
		}
		caption_INFO = null;
		OE_INFO = null;
		System.out.println("End of tranform and insert graphic info, number of files processed="+files.size() );
	}*/
	
	/*static void insertTITitle(Document docu)throws Exception{
		System.out.println("Start of inserting <title> for ti_title <info-obj>");
		NodeList lst = XMLUtil.xpathNodeSet(docu, "//info-obj[contains(@id, 'TITitle-')]");
		for(int i=0; i<lst.getLength(); i++){
			Element tiTitle = (Element)lst.item(i);
			if(tiTitle.hasAttribute("navTitle") || tiTitle.hasAttribute("yearRange")){
				String navTitle = tiTitle.getAttribute("navTitle");
				String yearRange = tiTitle.getAttribute("yearRange");
			}
		}
	}*/
	
	/**
	 * fix list enumeration  
	 */
	static void listEnum(Document doc) throws Exception{
		Node node = doc.getDocumentElement();
		listEnum(node, "arabicnum");
		listEnum(node, "loweralpha");
		listEnum(node, "arabicnum");
		listEnum(node, "loweralpha");
		NodeList lists = XMLUtil.xpathNodeSet(doc, "//list[@cleaned]");
		for(int i=0; i<lists.getLength(); i++){
			((Element)lists.item(i)).removeAttribute("cleaned");
		}
	}
	
	/**
	 * add enumType to top level unprocessed lists
	 */
	static void listEnum(Node node, String enumType) throws Exception{
		NodeList lists = XMLUtil.xpathNodeSet(node, "//list[not(ancestor::list[not(@cleaned)]) and not(@cleaned)]");
		for(int i=0; i<lists.getLength(); i++){
			Element list = (Element)lists.item(i);
			if(list.getAttribute("type").equalsIgnoreCase("ordered")){
				list.setAttribute("enumtype", enumType);
			}
			list.setAttribute("cleaned", "1");
		}
	}

	
	/**
	 * insert generated_id and graphic captions from ss1819.VV_OE table
	 */
	static void insertGXInfo(Document doc) throws Exception{
		System.out.println("Start of inserting graphic captions");
		if(OE_INFO==null){
			OE_INFO = loadOEInfoFromDB();
		}
		if(caption_INFO==null){
			caption_INFO = loadCaptions();
		}
		NodeList lst = XMLUtil.xpathNodeSet(doc, "//graphic");
		Element gx;
		for(int i=0; i<lst.getLength(); i++){
			gx = (Element)lst.item(i);
			//populate generated_id
			String oename = gx.getAttribute("oename");
			//pl("oename="+oename);
			//bad graphic names, such as 0900c8af80120403_0_0.gif
			if(!OE_INFO.containsKey(oename)){
				invalidGXs.add(oename);
				if(gx.getParentNode().getNodeName().equals("figure")){
					XMLUtil.xpathCommentOut(gx.getParentNode(), "");
				}else{
					XMLUtil.xpathCommentOut(gx, "");
				}
				System.err.println("WARNING! invalid graphic name found!! oenmae="+oename);
				continue;
			}
			
			String gid = OE_INFO.get(oename);
			if(oename.endsWith(".jpeg")){
				gx.setAttribute("oename", oename.replace(".jpeg", ".jpg"));
			}
			
			gx.setAttribute("graphicname",  gid);//TODO resume me for productoin 
			//gx.setAttribute("graphicname", "VV108406");
			
			//populate caption
			oename = oename.replace(".jpeg", ".jpg");
			String caption = caption_INFO.get(oename);
			if(caption==null && oename.contains(".")){
				caption = caption_INFO.get(oename.substring(0, oename.lastIndexOf(".")));
			}
			
			if(caption != null){
				Element captionNode = (Element)XMLUtil.xpathNode(gx, "/following-sibling::caption");
				//there are some graphics in tables which have no <caption>
				if(captionNode != null){
					captionNode.setTextContent(caption);					
				}
			}
		}
		System.out.println("End of inserting graphic captions; number of graphics affected="+lst.getLength());
	}
	
	/**
	 * Populate VV_OE table with graphic captions
	 * return these oename to caption mapping 
	 */
	static Map<String, String> loadCaptions() throws Exception{
		System.out.println("Starting to collect graphic captions MEPS");
		 Map<String, String> map = new HashMap<String, String>();
		CallableStatement stmt;
		if(meps == null){
			meps = new MEPSUtil(MEPS_DB);
		}
	    stmt = meps.con.prepareCall("{ call Volvo.getcaptions }");
	    stmt.execute();
	    
	    Statement st = meps.con.createStatement();
		String query = "select oe_name, caption from vv_oe";
		ResultSet rs = st.executeQuery(query);
		String oe_name, caption;
		while (rs.next()) {
				oe_name = rs.getString(1);
				caption = rs.getString(2);
				map.put(oe_name, caption);
		}
	    System.out.println("End of collecting collect graphic captions MEPS, number of captions = "+map.size());
	    return map;
	}
	
	/**
	 * insert each XML fragment into article XMLs
	 */
	void insertXMLFragments() throws Exception {
		System.out.println("Start inserting XML fragments into article XMLs");
		
		//getAllTocFragIDs();
		
		List<String> files = FileUtil.getAllFilesWithCertainExt(splitTocDir, ".xml");

		/*The following two statements used to get rid of redundant xml fragments from orphan artilces after all test procedures are processed (which add xml fragments which could exist in orphan artilces)
		 * the sorting step is to ensure orphan articles will be processed after all other artilces expect DTCIndex artilces (which should have no references pointing to any orphan articles)
		 */
		Collections.sort(files);
		Set<String> fragAdded = new HashSet<String>();
		
		for(String file: files){
			tempFileName = file;
			if(file.contains("DTCIndex")){
				//pl(" Moving DTCIndex file "+splitTocDir + file + " to "+fragInsertedDir + file);
				FileUtil.copyFile(splitTocDir + file, fragInsertedDir + file);
				continue;
			}
			pl(" Inserting XML fragments for file "+file);
			Document doc = XMLUtil.parseFile(splitTocDir + file);
			NodeList lst = XMLUtil.xpathNodeSet(doc, "//nevisid");
			for(int i=0; i<lst.getLength(); i++){
				Element nevisid = (Element)lst.item(i);
				String xmlFragId = nevisid.getAttribute("id");
				
				
//pl("  xmlFragId="+xmlFragId);
				
				//inside Orphan articles, by this time, all other articles (except DTCIndex article) should already been processed
				if(file.contains("-Orp-")){
					//this orphan xml fragment already inserted into a test procedure component earlier
					if(fragAdded.contains( "en-US"+xmlFragId )){
						XMLUtil.xpathCommentOut(nevisid, "");
						pl("Orphan fragment not needed and removed; fragment="+xmlFragId);
						continue;
					}
				}
				
				String  xmlFrag = xmlFragId + ".xml";
				String fragParentFile= null;
				String xmlFragNewID = null;
				if(fragMap.get(xmlFrag)!=null){
					fragParentFile = fragMap.get(xmlFrag)[0];
					xmlFragNewID = fragMap.get(xmlFrag)[1];
				}
				Node parent = nevisid.getParentNode();
				
				List<String> childFrags = new ArrayList<String>();
				//NodeList orphans = XMLUtil.xpathNodeSet(ele, "/exclusiveOrphan", "/decedents/decedent[@conditionType='test']");
				NodeList orphans = XMLUtil.xpathNodeSet(nevisid, "/exclusiveOrphan");
				for(int j=0; j<orphans.getLength(); j++){
					String fragID = ((Element)orphans.item(j)).getAttribute("id");
					if(!childFrags.contains(fragID)){
						childFrags.add(fragID);	
					}
				}
				
				/*
				//insert duplicate xml fragment content
		    	String fragFile = XMLLibDir + MEPSUtil.getThreeLayerDir(xmlFragId) + xmlFragId + ".xml";
				Document xmlFragDoc = XMLUtil.parseFile(fragFile);
				Node fragContent = doc.importNode(xmlFragDoc.getDocumentElement(), true);
				if(fragParentFile==null){
					fragMap.put(xmlFrag, file);
				}else{
					Node comment = doc.createComment("dup <info-obj> of file "+fragParentFile);
					fragContent.insertBefore(comment, fragContent.getFirstChild());
				}
				parent.insertBefore(fragContent, ele);
				parent.removeChild(ele);
*/
				
				
				//fragment never loaded before
				if(fragParentFile==null){
					//System.out.println("inDir + xmlFrag"+inDir + xmlFrag);
					Document xmlFragDoc = getXMLFragments(xmlFragId, childFrags);
					if(xmlFragDoc == null){
						continue;
					}

					Element fragContent = (Element)doc.importNode(xmlFragDoc.getDocumentElement(), true);
					//en-US0900c8af80420b5d-KC01416574
					
					//fragContent.setAttribute("documentType", ((Element)ele).getAttribute("documentType"));
					fragContent.setAttribute("yearRange", ((Element)nevisid).getAttribute("yearRange"));
					parent.insertBefore(fragContent, nevisid);
					
					Node servinfosubTitle = null ;
					
					//Changed on Feb 23, 2012
					//String newID = fragContent.getAttribute("id");
					String newID ;
					if(fragContent.getNodeName().equals("servinfosub")){
						newID = fragContent.getAttribute("id");
						servinfosubTitle = XMLUtil.xpathNode(fragContent, "/title[1]");
					}else{
						//in this special case (such as <i2> fragment) we only expect there is one servinfosub
//pl("fragContent == " + XMLUtil.xmlToStringNoHeader(fragContent));
						NodeList lst1 = XMLUtil.xpathNodeSet(fragContent, "//servinfosub");
						//in this special case (such as <i2> fragment) we only expect there is one servinfosub
						//TODO check if we need process more than one servinfosub
						Element servinfosub = (Element) (lst1.item(0));

	
//if(lst1.getLength() > 1) el("More than on servinfosub found: count = " + XMLUtil.xpathNodeSet(fragContent, "//servinfosub").getLength());
						
						if(servinfosub!=null){
							newID = servinfosub.getAttribute("id");
							servinfosubTitle = XMLUtil.xpathNode(servinfosub, "/title");
							pl("non-servinfosub fragment servinfosub newID=="+newID+"/n");
							//pl("non-servinfosub fragment servinfosub newID=="+newID+"/n"+XMLUtil.xmlToStringNoHeader(fragContent));
						}else{
							newID = "FragWillBeRemoved";
							pl("Fragment will be removed, type=" +fragContent.getNodeName() + " xmlFragId="+xmlFragId );
						}
					}
					
					if(servinfosubTitle!=null)
						servinfosubTitle.setTextContent(nevisid.getAttribute("title"));
					parent.removeChild(nevisid);
					
					String[] values = {file, newID};
					fragMap.put(xmlFrag, values);
				}
				//fragment already loaded before, insert a <intxref> or <extxref>
				else{
					Element xref;

					/*String xrefText = ele.getAttribute("title");
					if(file.equalsIgnoreCase(fragParentFile)){
						xref = doc.createElement("intxref");
						xref.setAttribute("refid", "en-US" + xmlFragId);
						xref.setAttribute("dest", "info-obj");
						xref.setTextContent(xrefText.trim());
					}
					//external reference (<extxref document="08GS350_01-00" filetype="sgml" extrefid="S22">Diagnostics</extxref>)
					else{
						xref = doc.createElement("extxref");
						xref.setAttribute("document", file.replace(".xml", ""));
						xref.setAttribute("filetype", "sgml");
						xref.setAttribute("extrefid", "en-US" + xmlFragId);
						xref.setTextContent(xrefText.trim());
					}*/
					if(!xmlFragNewID.equals("FragWillBeRemoved")){
						xref = doc.createElement("xref");
						xref.setAttribute("refid", xmlFragNewID);
						if(xmlFragNewID==null || xmlFragNewID.trim().length() < 2)pl("anormal xmlFragNewID="+xmlFragNewID);
						xref.setAttribute("dupFrag", "true");
						
						Node ptxt = doc.createElement("ptxt");
						ptxt.appendChild(xref);
						parent.insertBefore(ptxt, nevisid);
						parent.removeChild(nevisid);	
					}else{
						pl("FragWillBeRemoved frag="+xmlFragId);
						parent.removeChild(nevisid);
					}
				}
				//System.out.println(XMLUtil.xmlToString(parent));
			}
			
			processTestProc(doc, fragAdded);
			
			solveRefs(doc);
			
			//split over-sized articles
			int fileSize = XMLUtil.xmlToString(doc).length();
			pl("file size=" + fileSize);
			if(fileSize >= MAX_ARTICLE_SIZE){
				//int numOfFiles = (int)Math.ceil(((float)fileSize)/MAX_ARTICLE_SIZE);
				int numOfFiles = fileSize/MAX_ARTICLE_SIZE + 1;
				pl("numOfFiles:"+numOfFiles);
				int sizePerFile = fileSize/numOfFiles;
				int size = 0;
				int fileNo = 0;
				//NodeList servinfosubs = doc.getElementsByTagName("servinfosub");
				NodeList tiTitleChildren = XMLUtil.xpathNodeSet(doc, "//ti_title/*");
				for(int i=0; i<tiTitleChildren.getLength(); i++){
					Element child = (Element)tiTitleChildren.item(i);
					size += XMLUtil.xmlToString(child).length() - "<?xml version=\"1.0\" encoding=\"UTF-8\"?>".length();
					if(size > sizePerFile){
						size = 0;
						fileNo++;
					}
					
					//if this <ti_title> only has two children, let's put its second child in the same article with its first child; Sep 23
					int numOfKids = XMLUtil.xpathNodeSet(child, "/../*").getLength();
					if(numOfKids==2 && child.getNextSibling()==null){
						child.setAttribute("group", ((Element)child.getPreviousSibling()).getAttribute("group"));
					}else{
						child.setAttribute("group", fileNo + "");
					}
					
				}
				for(int i=0; i<=fileNo; i++ ){
					Node newTOCItem = doc.cloneNode(true);
					//doc.importNode(newTOCItem, true);
					//node.getParentNode().insertBefore(newTOCItem, node);
					XMLUtil.xpathRemove(newTOCItem, "//ti_title/*[not(@group='" + i + "')]");
					//the following statment could remove DTC code <ti_title> element
					XMLUtil.xpathRemove(newTOCItem, "//ti_title[count(*)=0]");
//XMLUtil.xpathRemove(newTOCItem, "//ti_title[count(parentFrags) = count(*)]");	//exclusivly for orphan fragment artilce				
					XMLUtil.xpathRemoveAtt(newTOCItem, "group", "//ti_title/*");
					//FileUtil.writer(fragInsertedDir + file.replace(".xml", "-" + (char)(97+i) + ".xml") , XMLUtil.xmlToString(newTOCItem));
					FileUtil.writer(fragInsertedDir + file.replace(".xml", "-" + i + ".xml") , XMLUtil.xmlToString(newTOCItem));
				}
			}else{
				FileUtil.writer(fragInsertedDir + file, XMLUtil.xmlToString(doc));
			}
		}
		fragMap = null;
		fragCache.clear();
		System.out.println("\nEnd of inserting XML fragments into article XMLs");
	}
	
	/**
              <servinfo IE-ID="en-US0900c8af80c58d5a" docno="VCC-150533" id="en-US0900c8af80c58d5a-KC02734639" xml:lang="en-US" yearRange="2004-2012">
<title>Accessory electronic module (AEM)</title>
<ref id="en-US0900c8af80c58d5a-KC05339419"/>
<ref id="en-US0900c8af80c58d5a-KC05339420"/>
	 */
	void solveRefs(Document doc) throws Exception{
		pl("Solving Servinfo refs");
		//collect all <ref> inside servinfo or servinfosub, seems there are only four elements 
		// which could have child <ref>, xref, torque, servinfo and servinfosub. 
		// there are also very a few item or ptxt which have <refs>
		NodeList refs = XMLUtil.xpathNodeSet(doc, "//ref");
		
		if(refs.getLength() == 0){
			pl("There are no servinfo refs in this article");
			return;
		}
		Set<String> nevisIds = new HashSet<>();
		for(int i=0; i<refs.getLength(); i++){
			Element ref = (Element)refs.item(i);
			String id = ref.getAttribute("id"); //en-US0900c8af80c58d5a-KC05339419, en-US0900c8af84fdc2c2-nev20688606n1-nev15841649n244
			id = id.replace("en-US", "");
			id = id.substring(0, id.indexOf("-")  ); //0900c8af80c58d5a, 0900c8af84fdc2c2
			nevisIds.add("'" + id + "'");
		}
		String ids = nevisIds.stream().collect(Collectors.joining(","));
		String query = "select distinct b.nevisId parentNevisid, a.elementFrom, b1.nevisId childNevisid, a.elementTo, cast(tb.yearRange as nvarchar(max)) as yearRange "
				+ "from S_DocumentLink a join dbo.S_Document b on a.fkDocument = b.id " 
				+ "join  dbo.S_Document b1 on a.projectDocumentTo = b1.projectDocumentId "
				+ "join dbo.S_DocumentContent dc on b1.id = dc.fkDocumentID "
				+ "left join vv.tocBuilder tb on b1.nevisId = tb.DOC_NevisId "
				+ "join dbo.stg_documents sd on b1.id = sd.fkDocument "
				+ "where b.nevisId in (" + ids + ") and dc.fkLanguage = 15";
		pl("solveServInfoRef query=" + query);
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery(query);
		
		/**
		 * could be multiple destinations, each destination associated with different yearRange, for example: 0900c8af844d72c-nev13717728n1-nev13717726n34 has three destinations
		0900c8af844d72c8	nev13717728n1-nev13717726n34	0900c8af844cc96e	nev13713573n1	20052006
		0900c8af844d72c8	nev13717728n1-nev13717726n34	0900c8af844cc978	nev13713573n1	2004
		0900c8af844d72c8	nev13717728n1-nev13717726n34	0900c8af844cc9b8	nev13713573n1	200720082009201020112012
		 */
		Map<String, Set<String>> map = new HashMap<>();
		while (rs.next()) {
			String parentNevisid  = rs.getString(1);
			String elementFrom  = rs.getString(2);
			String targetDocument = rs.getString(3);
			String elementTo = rs.getString(4);
			String yearRange = rs.getString(5); 
			
			String refFrom = parentNevisid + "-" + elementFrom;
			String refTo = (elementTo==null)?targetDocument:targetDocument + "-" + elementTo;
			refTo = refTo + "(" + yearRange + ")" ; //if yearRange is null, which means the dest document is not in vv.tocBuilder table
			map.putIfAbsent(refFrom, new HashSet<String>());
			map.get(refFrom).add(refTo);
		}
		
		//solve refs
		Set<String> unSolvedIds = new HashSet<>();
		for(int i=0; i<refs.getLength(); i++){
			Element ref = (Element)refs.item(i);
			String id = ref.getAttribute("id").replace("en-US", ""); //0900c8af80c58d5a-KC05339419
			if(map.get(id) != null){
				String destStr = map.get(id).stream().map(dest -> ("en-US" + dest)).collect(Collectors.joining(","));
				pl("solve  ref, count=" + map.get(id).size() + " ref=" + id + ",  ==> " + destStr);
				ref.setAttribute("id", destStr);
			}else{
				pl("unable to solve ref within stg_document table: ref=" + id );
				unSolvedIds.add(id);
			}
		}
		
		//let's try to solve remaining ones out of stg_document table
		if(unSolvedIds.size() > 0){
			solveRefsAccrossAllDocuments(unSolvedIds);
		}
	}
	
	private void solveRefsAccrossAllDocuments(Set<String> unSolvedIds)throws Exception{
		pl("start solveRefsAccrossAllDocuments, count=" +  unSolvedIds.size() + ", unSolvedIds= " + unSolvedIds);
		String nevisIDs = unSolvedIds.stream().map(id -> id.replace("en-US", "")).map(id -> id.substring(0, id.indexOf("-"))).map(id -> "'" + id + "'").collect(Collectors.joining(","));
		String query = "select distinct b.nevisId parentNevisid, a.elementFrom, b1.nevisId childNevisid, a.elementTo "
				+ "from S_DocumentLink a join dbo.S_Document b on a.fkDocument = b.id " 
				+ "join  dbo.S_Document b1 on a.projectDocumentTo = b1.projectDocumentId "
				+ "join dbo.S_DocumentContent dc on b1.id = dc.fkDocumentID "
				+ "where b.nevisId in (" + nevisIDs + ") and dc.fkLanguage = 15";
		pl("solveServInfoRef query=" + query);
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery(query);
		
		Map<String, Set<String>> map = new HashMap<>();
		while (rs.next()) {
			String parentNevisid  = rs.getString(1);
			String elementFrom  = rs.getString(2);
			String targetDocument = rs.getString(3);
			String elementTo = rs.getString(4);
			
			String refFrom = parentNevisid + "-" + elementFrom;
			String refTo = (elementTo==null)?targetDocument:targetDocument + "-" + elementTo;
			map.putIfAbsent(refFrom, new HashSet<String>());
			map.get(refFrom).add(refTo);
		}
		
		Iterator<String> it = unSolvedIds.iterator();
		while(it.hasNext()){
			String id = it.next();
			if(map.get(id) != null){
				pl("solve ref across all documents: count=" + map.get(id).size() + ", id=" +id + " ==> " + map.get(id));
			}else{
				pl("unable to solve ref across entire database: ref=" + id );
			}

		}
	}
	
	/**
	 * recursively load all descendant xml fragments 
	 */
	/*void loadAllDescendants(List<String> allFrags, List<String> discardFrags, String fragID) throws Exception{
		Document frag = getXMLFragments(fragID, null);
		refID2DocumentID(frag);
	    NodeList refs = XMLUtil.xpathNodeSet(frag, "//href[ancestor::xref]");
	    for(int i=0; i<refs.getLength(); i++){
	    	//en-US0900c8af8006ff9e#KC01107005
	    	String ref = ((Element)refs.item(i)).getTextContent();
	    	//pl("ref="+ref);
	    	if(ref.contains("#")){
		    	ref = ref.substring(0, ref.indexOf("#"));
	    	}
	    	if(!allFrags.contains(ref) && !discardFrags.contains(ref)){
	    		Document refDoc = getXMLFragments(ref, null);
	    		Element diagnosticEle = (Element)XMLUtil.xpathNode(refDoc, "//diagnostic");
	    		//pl("diagnosticEle==null"+(diagnosticEle==null)+" refDoc==null"+XMLUtil.xmlToStringNoHeader(refDoc));
	    		if(diagnosticEle!=null && diagnosticEle.getAttribute("type").equalsIgnoreCase("test")){
	    			allFrags.add(ref);
	    			loadAllDescendants(allFrags,discardFrags, ref);
	    		}else{
	    			discardFrags.add(ref);
	    		}
	    	}
	    }
	}*/
	
	
	
	/**
	 * get all TOC level xml fragment IDs
	 */
	void getAllTocFragIDs()throws Exception{
		System.out.println("Start of getting all TOC level xml fragment IDs");
		if(refMap!=null)
			return;
		//borrow this refMap object to hold existing fragments
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery("select distinct DOC_Nevisid from VV.TOCBuilder where year='"
				+ year + "' and model='" + model_fullName + "'");
		
		int count=0;
		while (rs.next()) {
			String id  = rs.getString(1).trim();
			refMap.put(id, null);
			count++;
		}
		System.out.println("End of getting all TOC level xml fragment IDs, number of IDs = "+count);
	}
	
	/**
	 * to process all test procedures in a file
	 *  
	 */
	void processTestProc(Document doc, Set<String> fragAdded)throws Exception{
		System.out.println("Start of processing test procedure file "+new Date());
		
		NodeList procs = XMLUtil.xpathNodeSet(doc, "//ti_title/servinfosub[diagnostic[@type='test']]");
		List<String> existingFrags = new ArrayList<String>();
		count = 1000;
		for(int i=0; i<procs.getLength(); i++){
			Element proc = (Element)procs.item(i);
			String fragID = proc.getAttribute("IE-ID");
			if(badProc.contains(fragID)){
				continue;
			}
			
			//temporarily move out its excluvsive orphans, whill insert them back after the test procedure is complete
			NodeList exclusiveOrphans = XMLUtil.xpathNodeSet(proc, "/*[@docno]");
			for(int j=0; j<exclusiveOrphans.getLength() ; j++){
				Element orphan = (Element)exclusiveOrphans.item(j);
				orphan.setAttribute("exOrphan", "true");
				doc.getDocumentElement().appendChild(orphan);
			}
			
			
			List<String> allFrags = loadAllTestDescendants(proc);
			fragAdded.addAll(allFrags);
			unifyResActionEles(proc);

			preProcessTestProcEleIDs(proc);

			resolveDynamicLinks(proc, allFrags);
			simplifyTestProc(proc);
			//pl("proc 1"+XMLUtil.xmlToStringNoHeader(proc));
			proc = uniqueFragIDs(proc, existingFrags);
			//pl("proc 2"+XMLUtil.xmlToStringNoHeader(proc));			
			//test if this test proc is self-contained (no branching links pointing to outside this procedure)
			
			//these fragments are called by <diagcallout>, but they are NOT test procedures
			List<String> nonTestFrags = new ArrayList<String>();
			nonTestFrags.add("en-US0900c8af8352b8e4");
			
		
			//badEles.add("en-US0900c8af8091fb06-KC02474817");
			
			
			
			NodeList xrefs = XMLUtil.xpathNodeSet(proc, "//action//intxref");
			//pl("xrefs found="+xrefs.getLength());
			for(int j=0; j<xrefs.getLength(); j++){
				Element xref = (Element)xrefs.item(j);
				xref.removeAttribute("callOutReturn");
				String id = xref.getAttribute("refid");
				String refFragID = id.substring(0, 21);
				//pl("id found="+id + " refFragID="+refFragID);pl("2proc="+XMLUtil.xmlToStringNoHeader(proc));
				if(!(nonTestFrags.contains(refFragID) || badEles.contains(id))){
					if(XMLUtil.xpathNode(proc, "[@id='"+id+"']", "//*[@id='"+id+"']")==null){
						//throw new Exception("broken id found="+id + " refFragID="+refFragID+" 2proc="+XMLUtil.xmlToStringNoHeader(proc));
						System.err.println("broken id found="+id + " refFragID="+refFragID+" 2proc="+XMLUtil.xmlToStringNoHeader(proc));
					}
				}
			}
			
			//pl("before:"+XMLUtil.xmlToStringNoHeader(proc));
			//put excluvsive orphans back
			exclusiveOrphans = XMLUtil.xpathNodeSet(doc.getDocumentElement(), "/*[@exOrphan]");
			for(int j=0; j<exclusiveOrphans.getLength() ; j++){
				Element orphan = (Element)exclusiveOrphans.item(j);
				String docno = orphan.getAttribute("docno") ;
				//this exclusive orphan might be part of test procedures and already inserted into this procedure by previous statements
				if(XMLUtil.xpathNodeSet(proc, "/*[@docno='" + docno + "']").getLength()==0){
					proc.appendChild(orphan);
				}else{
					doc.getDocumentElement().removeChild(orphan);
				}
			}
			//pl("after:"+XMLUtil.xmlToStringNoHeader(proc));
		}
		
		//remove unnecessary question and answers parts
		String[] pharses = {"Do you want to exit fault-tracing?", 
				"Do you want to view the information again?",
				"Do you want to view information again?",
				"Do you want to terminate fault-tracing at this time?"};
		for(int i=0; i<pharses.length; i++){
			String pharse = pharses[i];
			XMLUtil.xpathCommentOut(doc, "//question[contains(text()[1], '" + pharse + "') or " +
					"contains(phrase[1]/text()[1], '" + pharse + "')]/following-sibling::resact-grp", 
					"//question[contains(text()[1], '" + pharse + "') or " +
					"contains(phrase[1]/text()[1], '" + pharse + "')]");
		}
		
		if(count>3000){
			throw new Exception("Number of duplicated fragments is " + (count - 1000));
		}
		pl("Number of duplicated fragments is " + (count - 1000));
		System.out.println("End of processing test procedure file "+new Date());
	}
	
	/**
	 * to make each duplicate fragment id unique within this document
	 */
	Element uniqueFragIDs(Element proc, List<String> existingFrags) throws Exception{
		System.out.println("Start of uniquering test procedure IDs");
		
		//changed on Apr 5, 2012
		XMLUtil.xpathRemoveAtt(proc, "id", "//diagnostic[not(@docno) and @dupid]");
		//XMLUtil.xpathRemoveAtt(proc, "id", "//diagnostic[not(@docno)]");
		
		NodeList lst = XMLUtil.xpathNodeSet(proc, "/.", "/servinfosub", "/diagnostic[@docno]");
		Map<String,String> idMap = new HashMap<String, String>();
		for(int i=0; i<lst.getLength(); i++){
			Element ele = (Element)lst.item(i);
			String id = ele.getAttribute("IE-ID") ;
			if(existingFrags.contains(id)){
				String newID = id + ++count;
				idMap.put(id, newID);
			}else{
				existingFrags.add(id);
			}
		}
		String procContent = XMLUtil.xmlToStringNoHeader(proc);
		Iterator<String> it = idMap.keySet().iterator();
		while(it.hasNext()){
			String id = it.next();
			String newID = idMap.get(id);
			
			/*//IE-ID="en-US0900c8af80491ed5"
			procContent = procContent.replaceAll(id + "\"", newID + "\"");
			//id="en-US0900c8af8042a8c6-KC01586176
			procContent = procContent.replaceAll(id + "-", newID + "-");*/
			//replace all id as a string which is not followed by a digit
			procContent = procContent.replaceAll(id + "(?!\\d)", newID );
		}
		Node newProc = proc.getOwnerDocument().importNode(XMLUtil.parseStr(procContent).getDocumentElement(), true);
		proc.getParentNode().replaceChild(newProc, proc);
		return (Element)newProc;
	}
	
	/**
	 * 1. rename all <rescall-grp> to <resact-grp>, all <calloutaction> to <action> to simply following process steps
	 * 2. remove all  <action j2008use="0"> elements, they are not used.
	 */
	void unifyResActionEles(Element ele) throws Exception{
	    NodeList lst = XMLUtil.xpathNodeSet(ele, "//rescall-grp", "//calloutaction");
	    for(int i=0; i<lst.getLength(); i++){
	    	Element node  = (Element)lst.item(i);
	    	if(node.getNodeName().equalsIgnoreCase("rescall-grp")){
		    	ele.getOwnerDocument().renameNode(node, "", "resact-grp");
	    	}else if(node.getNodeName().equalsIgnoreCase("calloutaction")){
	    		ele.getOwnerDocument().renameNode(node, "", "action");
	    	}
	    }
	    XMLUtil.xpathCommentOut(ele, "//action[@j2008use='0']", "//action[@screenuse='0']");
	    XMLUtil.xpathCommentOut(ele, "//resact-grp[not(action)]");
	}
	
	/**
	 * recursively load all test descendant xml fragments 
	 */
	List<String> loadAllTestDescendants(Element proc) throws Exception{
		String procID = proc.getAttribute("IE-ID");
		System.out.println(" Start of loading all test descendant xml fragments " + procID + ";"+new Date());
		//insertXMLFragments(xmlFragId, childFrags);
		List<String> allFrags = new ArrayList<String>();
		List<String> discardFrags = new ArrayList<String>();
		loadAllTestDescendants(allFrags,discardFrags, procID);
		allFrags.remove(procID);
		for(String s:allFrags){
			//pl("allFrags="+s);
			Node frag = XMLUtil.xpathNode(proc, "/*[@IE-ID='" + s + "']");
			//test fragment might already exist as exclusive orphan inserted by a previous process
			if(frag == null){
				Element descendant = getXMLFragments(s, null).getDocumentElement();
				descendant = (Element)proc.getOwnerDocument().importNode(descendant, true);
				proc.appendChild(descendant);
			}else{
				pl("frag " + s + " not duplicated since it already exists as exclusive orphan");
			}
		}
		System.out.println("End of loading all test descendants "+new Date());
		return allFrags;
	}
	
	/**
	 * recursively load all test descendant xml fragments 
	 */
	void loadAllTestDescendants(List<String> allFrags, List<String> discardFrags, String fragID) throws Exception{
		Document frag = getXMLFragments(fragID, null);
		//refID2DocumentID(frag);
	    NodeList refs = XMLUtil.xpathNodeSet(frag, "//diagcallout/xref", "//resact-grp/action[@screenuse='1']//xref", "//rescall-grp/calloutaction//xref");
	    for(int i=0; i<refs.getLength(); i++){
	    	//en-US0900c8af8006ff9e-KC01107005
	    	String ref = ((Element)refs.item(i)).getAttribute("refid");
	    	if(ref.contains(IDJOIN)){
		    	ref = ref.substring(0, ref.lastIndexOf(IDJOIN));
	    	}
	    	//pl("after:ref="+ref);
if(ref.trim().length()<3)pl("frag="+XMLUtil.xmlToStringNoHeader(frag));
	    	
	    	if(!allFrags.contains(ref) && !discardFrags.contains(ref)){
	    		Document refDoc = getXMLFragments(ref, null);
	    		Element diagnosticEle = (Element)XMLUtil.xpathNode(refDoc, "//diagnostic");
	    		//pl("diagnosticEle==null"+(diagnosticEle==null)+" refDoc==null"+XMLUtil.xmlToStringNoHeader(refDoc));
	    		if(diagnosticEle!=null && diagnosticEle.getAttribute("type").equalsIgnoreCase("test")){
	    			//pl("adding to allFrags ref="+ref+"  fragID="+fragID);
	    			allFrags.add(ref);
	    			loadAllTestDescendants(allFrags,discardFrags, ref);
	    		}else{
	    			discardFrags.add(ref);
	    		}
	    	}
	    }
	}
	
	/**
	 * to simply test procedure 
	 *  1. surpress non-call-back diagcallout
	 */
	void surpressNonCallBackDiagcallouts(Element proc)throws Exception{
		System.out.println("Start of first simplifing test procedure steps "+proc.getAttribute("IE-ID"));
		String procFirstStepID = XMLUtil.xpathStr(proc, "/diagnostic[1]/testgrp[1]/@id");
		NodeList lst;
		int count = 0;
		//surpress non-call-back diagcallout
		lst = XMLUtil.xpathNodeSet(proc, "//diagcallout");
		for(int i=0; i<lst.getLength(); i++){
			Element ele = (Element)lst.item(i);
			NodeList resactGrps = XMLUtil.xpathNodeSet(ele, "/following-sibling::resact-grp[descendant::intxref]");
			boolean needCallBack = false;
			//if all resact-grps are 1. end of test procedures or 2. pointing back to the begining of test proc
			for(int j=0; j<resactGrps.getLength(); j++){
				Element resactGrp = (Element)resactGrps.item(j);
				String refid = XMLUtil.xpathStr(resactGrp, "//intxref/@refid");
				//only if it is not NEW TRY
				if(!refid.equalsIgnoreCase(procFirstStepID)){
					count++;
					needCallBack = true;
					//ele.setAttribute("isProcEnd", "true");
					break;
				}
			}
			if(needCallBack==false)
				//ele.getOwnerDocument().renameNode(ele, "", "fakeDiagcallout");
				ele.setAttribute("callback", "false");
		}
		System.out.println(" number of non-callback diagcallout found "+count);
	}
	
	/**
	 * to simply test procedure steps by
	 *  removing redundant link
	 */
	void simplifyTestProc(Element proc)throws Exception{
		System.out.println("Start of simplifing test procedure steps "+proc.getAttribute("IE-ID"));
		NodeList lst;
		
		//step 1: removing redundant resact-grp links
		/*                      <resact-grp id="en-US0900c8af80422682-resGrpID4">
                                    <result comp="string" highlight="0" op="eq" return="FAULTTRACING FAILED"/>
                                    <action>
                                        <return value="FAULTTRACING FAILED"/>
                                        <intxref dest="lst-itm"  refid="en-US0900c8af80422625-resGrpID2"/>
                                    </action>
                                </resact-grp>*/
		Map<String, String> refMap = new HashMap<String,String>();
		//some resact-grp have ids in source xml fragments, such as en-US0900c8af84903405
		lst = XMLUtil.xpathNodeSet(proc, "//resact-grp[@callOutReturn]");
		for(int i=0; i<lst.getLength(); i++){
			Element ele = (Element)lst.item(i);
			String idFrom = ele.getAttribute("id");
			String idTo = XMLUtil.xpathStr(ele, "//intxref/@refid");
			if(refMap.containsKey(idFrom)){
				pl("Proc="+XMLUtil.xmlToStringNoHeader(proc));
				pl("idFrom=" + idFrom + " idTo="+ idTo );
				throw new Exception("duplicate resact-grp id found "+idFrom);
			}else{
				refMap.put(idFrom, idTo);
			}
		}
		XMLUtil.xpathCommentOut(proc, "//resact-grp[@callOutReturn]");
		lst = XMLUtil.xpathNodeSet(proc, "//intxref[@callOutReturn]");
		for(int i=0; i<lst.getLength(); i++){
			Element ele = (Element)lst.item(i);
			String id = ele.getAttribute("refid");
			String destId = id;
			do {
				destId = refMap.get(destId);
			}while(refMap.containsKey(destId));
			//no xref needed
			if(destId.length()==0){
				XMLUtil.xpathRemove(ele, "");
			}else{
				ele.setAttribute("refid", destId);
			}
		}

		/*step 2: removing all <testgrp> with <diagcallout> in it
		 *  content (if there are any) in this kind of <testgrp> will never be displayed to the users since the <diagcallout>
		 *  take users directly to its destination <testgrp>, thus, we can remove all <testgrp> with <diagcallout> in it 
		 */
		refMap.clear();
		lst = XMLUtil.xpathNodeSet(proc, "//testgrp[descendant::diagcallout]");
		String idFrom ;
		String idTo ;
		for(int i=0; i<lst.getLength(); i++){
			Element ele = (Element)lst.item(i);
			//seems only the first <testgrp> in a <diagnostic> has IE-IE. i.e  <testgrp IE-ID="en-US0900c8af8042a97f_KC01544678"
			if(ele.hasAttribute("IE-ID")){
				//its parent must be <diagnostic>, calling its parent <diagnostic> is same as calling this <testgrp>
				idFrom = ((Element)ele.getParentNode()).getAttribute("id");
				idTo = ele.getAttribute("id");
				refMap.put(idFrom, idTo);
			}
			idFrom = ele.getAttribute("id");
			if(ele.hasAttribute("deadEndDiagcallout")){
				idTo = "deadEndDiagcallout";
			}else{
				idTo = XMLUtil.xpathStr(ele, "//diagcallout/intxref/@refid");				
			}
			/*some diagcallout pointing to an non-existing fragment, we have to exclude them
			 * i.e in frag en-US0900c8af8042d816, one diagcallout is pointing to en-US0900c8af8042888c#KC01435614, which doesn't exist*/
			refMap.put(idFrom, idTo);
		}
		
		lst = XMLUtil.xpathNodeSet(proc, "//intxref[ancestor::action]");
		for(int i=0; i<lst.getLength(); i++){
			Element ele = (Element)lst.item(i);
			String id = ele.getAttribute("refid");
			String destId = id;
			while(refMap.containsKey(destId)){
				//pl("destID="+destId);
				destId = refMap.get(destId);
			}
			//pl("destID="+destId);
			if(destId.length()==0){
				pl("Proc="+XMLUtil.xmlToStringNoHeader(proc));
				pl("id="+id+ "  " + XMLUtil.getXpath(ele));
				throw new Exception("broken reference found");
			}else if(destId.equals("deadEndDiagcallout")){
				ele.setTextContent("Pointing to dead End diagcallout testgrp "+ele.getTextContent());
				XMLUtil.xpathCommentOut(ele,"");
			} else{
				ele.setAttribute("refid", destId);
			}
		}
		Node firstDiagcallout = XMLUtil.xpathNode(proc, "/diagnostic[1]/testgrp[1]//diagcallout");
		//move the xml fragment which contains the first real <testgrp> to the top 
		if(firstDiagcallout != null){
			String id = XMLUtil.xpathStr(firstDiagcallout, "//intxref/@refid");
			//it might points to an non-existing fragments and already commented out by previous process
			if(id.length()>0){
				//seems <diagcallout> most of time call a xml fragment directly, but sometimes it call its descendant <testgrp>
				
				//changed on Arp 5, 2012
				Node xmlFrag = XMLUtil.xpathNode(proc, "/*[@id='" + id + "']", "/*[descendant::testgrp/@id='" + id + "']", "/*[descendant::diagnostic/@id='" + id + "']");
				//Node xmlFrag = XMLUtil.xpathNode(proc, "/*[@id='" + id + "']", "/*[descendant::testgrp/@id='" + id + "']");
				
				Node firstDiagEle = XMLUtil.xpathNode(proc, "/diagnostic[1]");
				pl("id22="+id);pl("Proc="+XMLUtil.xmlToStringNoHeader(proc));
				proc.insertBefore(xmlFrag, firstDiagEle);
			}
		}
		//XMLUtil.xpathCommentOut(proc, "//diagcallout");
		XMLUtil.xpathCommentOut(proc, "//testgrp[descendant::diagcallout]");
		
		
		
		/*
		lst = XMLUtil.xpathNodeSet(proc, "//intxref[contains(@refid, 'resGrpID')]");
		for(int i=0; i<lst.getLength(); i++){
			Element ele = (Element)lst.item(i);
			String id = ele.getAttribute("refid");
			pl("simplifyTestProc() refid="+id);
			Element targetEle = (Element)XMLUtil.xpathNode(proc, "//resact-grp[@id='"+id+"']");
			Element tgtActionEle = (Element)XMLUtil.xpathNode(targetEle, "//action");
			Element sourceActionEle = (Element)XMLUtil.xpathNode(ele, "/ancestor::action");
			sourceActionEle.getParentNode().replaceChild(tgtActionEle, sourceActionEle);
			targetEle.setAttribute("deleteme", "true");
		}
		//these <rescall> or <resact> will never be used
		//XMLUtil.xpathCommentOut(proc, "//rescall-grp[not(@id)]", "//resact-grp[not(@id)]");
		XMLUtil.xpathCommentOut(proc, "//*[@deleteme]");
*/
		
		//step2: surpress diagcallout/diagcallout
		/*lst = XMLUtil.xpathNodeSet(proc, "//testgrp[count(*)=2 and title and (subcalloutnode or iecalloutnode)]");
		for(int i=0; i<lst.getLength(); i++){
			Element ele = (Element)lst.item(i);
		}*/
		
		
		//Stop "NEW TRY"; seems we can assume to stop the test procedure whenever we see <return value="NEW TRY">
		/*NodeList newTrys = XMLUtil.xpathNodeSet(proc, "//return[@value='NEW TRY']");
		for(int j=0; j<newTrys.getLength(); j++){
			Element newTry = (Element)newTrys.item(j);
			if(XMLUtil.xpathNode(newTry, "/intxref")==null) continue;
			//it should have one and only one child, an <intxref> added by previous code, which should points back to the procedure begining
			String refid = XMLUtil.xpathStr(newTry, "/intxref/@refid");
			Node comment = newTry.getOwnerDocument().createComment("NEW TRY (" + refid + ")");
			newTry.replaceChild(comment, newTry.getElementsByTagName("intxref").item(0));
		}*/
		
		/*seems <resact-grp> which's child <result> has no @button attribute (users have nothing to click on) can be removed*/
		//XMLUtil.xpathCommentOut(proc, "//resact-grp[result[not(@button)]]");
	}
	
	/*	void dupFrags(Node frag, String ancestorFrags, List<String> allFrags)throws Exception{
	//some fragments need more than one copy if they are referenced by <diagcallout> more than one time
	NodeList refs = XMLUtil.xpathNodeSet(frag, "//intxref[ancestor::diagcallout]");
	pl(refs.getLength() + " diagcallout found");
	char c = 'A';
    for(int i=0; i<refs.getLength(); i++){
    	Element refEle = (Element)refs.item(i);
    	//en-US0900c8af8006ff9e#KC01107005
    	String ref = refEle.getAttribute("refid");
    	//pl("ref="+ref);
    	if(ref.contains("-KC")){
	    	ref = ref.substring(0, ref.indexOf("-KC"));
    	}
    	if(allFrags.contains(ref)){
    		//first one
    		allFrags.remove(ref);
    	}else{
    		//duplicate copies needed, append a upper-case letter to its existing id to make a new ID
    		String newFragID = ref + c++;
    		pl("dup xml frag added "+newFragID);
    		refEle.setAttribute("refid", refEle.getAttribute("refid").replace(ref, newFragID));
    		Node originalFrag = XMLUtil.xpathNode(frag, "/*[@IE-ID='" + ref + "']");
			Node dup = XMLUtil.parseStr(XMLUtil.xmlToStringNoHeader(originalFrag).replace(ref, newFragID)).getDocumentElement();
			dup = frag.getOwnerDocument().importNode(dup, true);
			frag.appendChild(dup);
    	}
    }
	}*/

	/**
	 * to turns dynamic Links to M1's intxref links 
	 *  
	 */
	void resolveDynamicLinks(Element proc, List<String> allFrags)throws Exception{
		System.out.println("Start of turning dynamic Links to M1's intxref links "+proc.getAttribute("IE-ID"));
		
		surpressNonCallBackDiagcallouts(proc);
		dupFrags(proc, allFrags);
		
	/*
		Node startFrag = XMLUtil.xpathNode(proc, "/diagnostic[1]");
		count = 1000;
		dupFrags( startFrag, "",  allFrags, "");
		//these left fragments are not touched by privious process, need to be processed as well

		String frag = allFrags.get(0);
		Node fragNode = XMLUtil.xpathNode(proc, "/*[@IE-ID='" + frag + "']");
		dupFrags( fragNode, "",  allFrags, "");
	 */	
		NodeList callOuts = XMLUtil.xpathNodeSet(proc, "//diagcallout[not(@callback='false')]");
		int callOutID = 0;
		for(int j=0; j<callOuts.getLength(); j++){
			Element callOut = (Element)callOuts.item(j);
			String rootFragID = getRootFragID(callOut);
			//NodeList rescallGrps = XMLUtil.xpathNodeSet(callOut, "/../rescall-grp", "/../resact-grp");
			NodeList rescallGrps = XMLUtil.xpathNodeSet(callOut, "/../resact-grp");
			//assume each callout element (<diagcallout>) has one and only one <xref> child
			//String refID = XMLUtil.xpathStr(callOut, "/xref/ref/href/text()");
			String refID = XMLUtil.xpathStr(callOut, "/intxref/@refid");
			String testGrpTitle = XMLUtil.xpathStr(callOut, "/ancestor::testgrp/title/phrase/text()");
//pl("refID:"+refID);
			//references doen't exist, commented out by preProcessEleIDs()
			if(refID.length()<1){
				continue;
			}
//if(refID.equals("en-US0900c8af8085646810041001-KC01115099"))pl(XMLUtil.xmlToStringNoHeader(proc));
			//pl("refid="+refID + "  proc123="+XMLUtil.xmlToStringNoHeader(proc));
			//en-US0900c8af8006ffc3#KC01106972
			Element targetEle = (Element)XMLUtil.xpathNode(proc, "//diagnostic[@id=\"" + refID + "\"]",  "//diagnostic[testgrp/@id=\"" + refID + "\"]");
			//sometimes, it calls a nonexisting ids in a fragment, we need correct it
			if(targetEle==null){
				String IE_ID = refID.substring(0, refID.lastIndexOf(IDJOIN));
				targetEle = (Element)XMLUtil.xpathNode(proc, "//diagnostic[@IE-ID=\"" + IE_ID + "\"]");
				String correctID = targetEle.getAttribute("id");
				Element intxref = (Element)XMLUtil.xpathNode(callOut, "/intxref");
				intxref.setAttribute("refid", correctID);
				pl("Fixed a wrong diagcallout reference; refid="+refID+" correctID="+correctID);
			}
            //<return value="FAULTTRACING FAILED"/>
			NodeList returnEles = XMLUtil.xpathNodeSet(targetEle, "//return");
			for(int k=0; k<returnEles.getLength(); k++){
				Element returnEle = (Element)returnEles.item(k); 
				String value = returnEle.getAttribute("value");
				for(int m=0; m<rescallGrps.getLength(); m++){
					Element rescallGrp = (Element)rescallGrps.item(m);
					String returnValue = XMLUtil.xpathStr(rescallGrp, "/result/@return");
					//pl("value="+value+" returnValue="+returnValue);
					if(value.equalsIgnoreCase(returnValue)){
						if(!rescallGrp.hasAttribute("id")){
							rescallGrp.setAttribute("id", rootFragID + IDJOIN + "resGrpID"+ ++callOutID);
						}
						rescallGrp.setAttribute("callOutReturn", "true");
						String rescallGrpID = rescallGrp.getAttribute("id");
						Element xref = returnEle.getOwnerDocument().createElement("intxref");
						xref.setAttribute("refid", rescallGrpID);
						//xref.setAttribute("dest", "lst-itm");
						xref.setAttribute("dest", "info-obj");
						xref.setAttribute("callOutReturn", "true");
						//xref.setTextContent(testGrpTitle);
						Node actionEle = returnEle.getParentNode();
						Node existingIntxref = XMLUtil.xpathNode(actionEle, "/intxref");
						if(existingIntxref==null){
							actionEle.appendChild(xref);
						}else{
							Node newActionEle = actionEle.getOwnerDocument().importNode(actionEle.cloneNode(true), true);
							actionEle.getParentNode().appendChild(newActionEle);
							actionEle.replaceChild(xref, existingIntxref);
						}
					}
				}
			}
		}
	}
	
	/**
	 * some fragments need more than one copy if they are referenced by <diagcallout> more than one time
	 *  
	 */
	void dupFrags(Element proc, List<String> allFrags)throws Exception{
		//System.out.println("Start of duplicating necessary test fragments");
		//some fragments need more than one copy if they are referenced by <diagcallout> more than one time
		NodeList callOuts = XMLUtil.xpathNodeSet(proc, "//diagcallout[not(@callback='false')]");
		List<String> callees = new ArrayList<String>();
		for(int j=0; j<callOuts.getLength(); j++){
			Element callOut = (Element)callOuts.item(j);
			String refID = XMLUtil.xpathStr(callOut, "/intxref/@refid");
			if(refID.length()<1) continue;
			
			refID = refID.substring(0, 21);
			//pl("diagcallout map " + rootFragID + "  " + refID);
			if(!callees.contains(refID))
				callees.add(refID);
		}
		allFrags.add(proc.getAttribute("IE-ID"));
		List<String> headerFrags = new ArrayList<String>();
		for(int j=0; j<allFrags.size(); j++){
			String frag = allFrags.get(j);
			//header frag, not being referenced by anybody
			if(!callees.contains(frag)){
				headerFrags.add(frag);
			}
		}
		for(int j=0; j<headerFrags.size(); j++){
			String frag = headerFrags.get(j);
			Node fragNode = XMLUtil.xpathNode(proc, "/*[@IE-ID='" + frag + "']");
			dupFrags( fragNode, "",  allFrags, frag);
		}
		
		if(allFrags.size()!=0){
			//pl("1proc="+XMLUtil.xmlToStringNoHeader(proc));
			//throw new Exception("allFrag is not empty "+ allFrags.size() + " " + allFrags.get(0));
			System.err.println("WARNING! Not an error, but double check this fragment doesn't need call back to its caller. allFrag is not empty "+ allFrags.size() + " " + allFrags.get(0));
		}
	}
	
	
	void dupFrags(Node frag, String ancestorFrags, List<String> allFrags, String fragID)throws Exception{
		System.out.println("Start of duplicating fragment "+((Element)frag).getAttribute("IE-ID"));
	//some fragments need more than one copy if they are referenced by <diagcallout> more than one time
	NodeList refs = XMLUtil.xpathNodeSet(frag, "//intxref[ancestor::diagcallout[not(@callback='false')]]");
	//pl(refs.getLength() + " diagcallout found!");
	allFrags.remove(fragID);
    for(int i=0; i<refs.getLength(); i++){
    	Element refEle = (Element)refs.item(i);
    	//en-US0900c8af8006ff9e#KC01107005
    	String ref = refEle.getAttribute("refid");
    	//pl("call out ref==="+ref);
    	
    	//if(ref.contains("-KC")){ref = ref.substring(0, ref.indexOf("-KC"));}
    	//en-US0900c8af8376b466-nev12064694n1
    	//if(ref.contains("-nev")){ref = ref.substring(0, ref.indexOf("-nev"));}
    	//pl("frag="+XMLUtil.xmlToStringNoHeader(frag.getParentNode()));
    	String idtail = ref.substring(ref.lastIndexOf("-"));
    	ref = ref.substring(0, 21);
    	//pl("call out IE-ID ==="+ref);

    	if(ancestorFrags.contains(ref)){
    		throw new Exception("pointing back to its ancestor ref="+ref + " ancestoer="+ancestorFrags);
    	}else{
    		//pl("ref="+ref+" fragXML="+XMLUtil.xmlToStringNoHeader(frag.getParentNode()));
    		Node originalFrag = XMLUtil.xpathNode(frag, "/../*[@IE-ID='" + ref + "']");
    		
        	if(allFrags.contains(ref)){
        		//first one
        		//pl(ref + " before="+allFrags.size());
        		allFrags.remove(ref);
        		pl(ref + " removed");
            	dupFrags(originalFrag, ancestorFrags, allFrags, "");
        	}else{
        		//duplicate copies needed, append a upper-case letter to its existing id to make a new ID
        		String newFragID = ref + ++count;
        		//pl("dup xml frag added "+ newFragID + idtail);
        		refEle.setAttribute("refid", newFragID + idtail);
        		//refEle.setAttribute("refid", newFragID);
    			Node dup = XMLUtil.parseStr(XMLUtil.xmlToStringNoHeader(originalFrag).replace(ref, newFragID)).getDocumentElement();
    			dup = frag.getOwnerDocument().importNode(dup, true);
    			((Element)dup).setAttribute("dup", "true");
    			frag.getParentNode().appendChild(dup);
    			//pl("c="+count);
            	dupFrags(dup, ancestorFrags, allFrags, "");
        	}
    		}
    	}
	}
	
	/**
	 * replace various reference IDs with nevisid, which is the xml fragment id
	 * <xref id="nev10751697n179">
    		<ref id="nev10751700n186">
        		<href title="VCC-253442-1, Variable valve timing unit, checking and adjusting">refID not
            	found in dbo.document table en-US0900c8af8382b341#nev10753196n1</href>
    		</ref>
		</xref>
		
		<href title="VCC-143825-2, Throttle body (TB), cleaning">en-US0900c8af83d364b1#KC01112922</href>
		
	 */
	static void refID2DocumentID(Node refDoc)throws Exception{
		Statement stmt = con.createStatement();
		NodeList refs = XMLUtil.xpathNodeSet(refDoc, "//href[ancestor::xref]", "//href[ancestor::torque]");
		for(int i=0; i<refs.getLength(); i++){
			Element href = (Element)refs.item(i);
			//en-US0900c8af80426adf#KC01586846; en-US0900c8af80426adf
			String textCont = href.getTextContent();
			textCont = textCont.replace("#fail", "");
			String title = href.getAttribute("title");
			String vccNumber = "NOVCCNUMBER";
			if(title.startsWith("VCC-")){
				vccNumber = title.substring(0, title.indexOf(","));
				//pl("vccNumber="+vccNumber);
			}
			String refID;
			if(!textCont.contains("#")){ //en-US0900c8af80426adf
				refID = textCont.substring("en-US".length());
			}else{
				refID = textCont.substring("en-US".length(), textCont.lastIndexOf("#"));
			}
			String query = "select nevisid from dbo.S_Document where chronicleid = '" + refID 
			+ "' or nevisid='" + refID + "' or vccNumber='" + vccNumber + "'" ; 
			//pl("refID="+refID+" query="+query);
			ResultSet rs = stmt.executeQuery(query);
			if(rs.next()){
				String nevisid = rs.getString(1);
				//pl("nevisid="+nevisid);
				href.setTextContent(textCont.replace(refID, nevisid));
				if(rs.next()){
					throw new Exception("More than one record found in table dbo.s_document refID="+refID + " VCCNumber="+vccNumber);
				}
			}else{
				Element ele = (Element)XMLUtil.xpathNode(href, "/ancestor::xref", "/ancestor::torque");
				href.setTextContent("refID not found in dbo.s_document table "+ textCont);
				Node diagcallout = XMLUtil.xpathNode(ele, "/ancestor::diagcallout");
				Node testgrp = XMLUtil.xpathNode(ele, "/ancestor::testgrp");
				//some <xref> has more than one descedant <href>
				if(ele.getElementsByTagName("href").getLength()<=1){
					//this <testgrp> is a dead end testgrp, should never be referenced
					if(diagcallout != null && testgrp != null){
						((Element)testgrp).setAttribute("deadEndDiagcallout", "true");
					}
					//pl("comment out xref="+XMLUtil.xmlToStringNoHeader(xref));
					XMLUtil.xpathCommentOut(ele, "");
					//XMLUtil.xpathCommentOut("comment out href", xref);
				}else{
					//pl("comment out href, xref="+XMLUtil.xmlToStringNoHeader(xref));
					XMLUtil.xpathCommentOut(href, "");
					//XMLUtil.xpathCommentOut("comment out xref", xref);
				}
				System.err.println("refID not found in dbo.s_document table "+refID + "  " + XMLUtil.xmlToStringNoHeader(href));
			}
		}
	}
	/**
	 * 1. replace chronicleid with nevisid, which is the xml fragment id
	 * 2. fully qualify element ids by prepending xml fragment id
	 * 	i.e    <testgrp id="KC01240373">  ==>   <testgrp id="en-US0900c8af80856468-KC01240373">
	 * 3. dedup duplicated xref/ref/href
	 */
	static void preProcessEleIDs(Element frag)throws Exception{
		// 1. replace nevis/chronicleid with nevisid, which is the xml fragment id
		refID2DocumentID(frag);
		
		//fully qualify element ids by prepending xml fragment id
		String IE_ID = frag.getAttribute("IE-ID");
		NodeList lst = XMLUtil.xpathNodeSet(frag, "/.", "//*[@id]");
		for(int i=0; i<lst.getLength(); i++){
			Element ele = (Element)lst.item(i);
			String id = IE_ID + IDJOIN + ele.getAttribute("id");
			ele.setAttribute("id", id);
		}
		
		//process xref/ref/href and torque/ref/href references
		
		//lst = XMLUtil.xpathNodeSet(frag, "//xref", "//torque");
		lst = XMLUtil.xpathNodeSet(frag, "//xref[descendant::href]", "//torque[descendant::href]"); //modifed 3/10/2019
		
		
		for(int i=0; i<lst.getLength(); i++){
			Element ele = (Element)lst.item(i);
			//some <xref> are missing <href> descendants
			//pl("234 frag="+XMLUtil.xmlToStringNoHeader(frag));
			NodeList href = XMLUtil.xpathNodeSet(ele, "/descendant::href");
			//merge possible dup href, i.e en-US0900c8af80cda161
			if(href.getLength()==2){
				if(href.item(0).getTextContent().equalsIgnoreCase(href.item(1).getTextContent())){
					XMLUtil.xpathCommentOut(href.item(1), "");
				}
			}
			
			href = XMLUtil.xpathNodeSet(ele, "/descendant::href");
			
			if(href.getLength() == 0){
				Element diagCallout = (Element)XMLUtil.xpathNode(ele, "/ancestor::diagcallout");
				if(diagCallout != null){
					Element testgrp = (Element)XMLUtil.xpathNode(diagCallout, "/ancestor::testgrp");
					if(testgrp != null){
						testgrp.setAttribute("deadEndDiagcallout", "true");
					}
				}
				XMLUtil.xpathCommentOut(ele);
				
			//changed on March 26, 2012
			}else if(href.getLength() > 1){
				Element diagCallout = (Element)XMLUtil.xpathNode(ele, "/ancestor::diagcallout");
				if(diagCallout != null){
					/*Element testgrp = (Element)XMLUtil.xpathNode(diagCallout, "/ancestor::testgrp");
					if(testgrp != null){
						testgrp.setAttribute("deadEndDiagcallout", "true");
					}
					XMLUtil.xpathCommentOut(ele);*/

					//let's comment out all <href> but the first one
					XMLUtil.xpathCommentOut(ele, "/ref/href[position()!=1]");
					System.err.printf("diagCallout has more than one <href> found, file=%s; frag=%s; href[1]/text()=%s\n", tempFileName, IE_ID, href.item(0).getTextContent());
				}else{//dedup duplicated xref/ref/href
					XMLUtil.xpathCommentOut(ele, "/ref/href[preceding-sibling::href/text()=text()]");
				}
				processXref(ele);
				
			}else{
				
				processXref(ele);
				
			}
		}
	}
	
	/**
	 * Turn Volvo <xref> or <torque> into M1's <xref> 
	 */
	static void processXref(Element ele)throws Exception{
		NodeList hrefs = XMLUtil.xpathNodeSet(ele, "//href");
		for(int i=0; i<hrefs.getLength(); i++){
			Element href = (Element)hrefs.item(i);
			Element newXref;
			if(ele.getNodeName().equalsIgnoreCase("torque")){
				newXref = ele.getOwnerDocument().createElement("torque");
			}else{
				newXref = ele.getOwnerDocument().createElement("xref");	
			}
			
			String id = href.getTextContent();
			
			//comment out bad references
			if(badEles.contains(id)){
				XMLUtil.xpathCommentOut(href);
			}
			
			//these are bad ids, should be corrected
			if(id.equalsIgnoreCase("en-US0900c8af83b86680#nev12516920n3")){
				pl("1fixed id "+id);
				id = "en-US0900c8af83b86680#nev12516920n1";
			}
			if(id.equalsIgnoreCase("en-US0900c8af84916eec#nev14480293n3")){
				pl("2fixed id "+id);
				id = "en-US0900c8af84916eec#nev14480293n1";
			}
			//this id points to a <diagnostic> element, which normally has no id, thus, we redirect it to the first <testgrp> of this <diagnostic>
			if(id.equalsIgnoreCase("en-US0900c8af8091fb06#KC02474817")){
				pl("3fixed id "+id);
				id = "en-US0900c8af8091fb06#KC08818477";
			}
			
			id = id.replace("#", IDJOIN);
			String textContent = href.getAttribute("title");
			newXref.setAttribute("refid", id);
			newXref.setTextContent(textContent);
			ele.getParentNode().insertBefore(newXref, ele);
		}
		XMLUtil.xpathCommentOut(ele);
	}
	
	static void preProcessTestProcEleIDs(Element proc)throws Exception{
		//we added its parent servinfosub id as its id temporally
	    //to simplify the coding (its id is a duplicate id as its parent id, we will remove them later)
		//pl("preProcessTestProcEleIDs proc="+XMLUtil.xmlToStringNoHeader(proc));
		NodeList lst = XMLUtil.xpathNodeSet(proc, "//diagnostic[not(@IE-ID)]"); 
		for(int i=0; i<lst.getLength(); i++){
			Element ele = (Element)lst.item(i);
			//its parent should be a <servinfosub>
			String id = ((Element)ele.getParentNode()).getAttribute("id");
			String IE_ID = ((Element)ele.getParentNode()).getAttribute("IE-ID");
			
			//changed on Apr 5, 2012
			if(!ele.hasAttribute("id")){
				ele.setAttribute("id", id);	
				ele.setAttribute("dupid", "true");
			}
			//ele.setAttribute("id", id);
			ele.setAttribute("IE-ID", IE_ID);
		}
		
		//turn all <action> xref to <intxref> since each test proc must be self-contained
		lst = XMLUtil.xpathNodeSet(proc, "//action//xref", "//diagcallout//xref");
		for(int i=0; i<lst.getLength(); i++){
			Element ele = (Element)lst.item(i);
			ele.getOwnerDocument().renameNode(ele, "", "intxref");
			ele.setAttribute("dest", "info-obj");
		}
		
		//pl("preProcessTestProcEleIDs proc2="+XMLUtil.xmlToStringNoHeader(proc));
	}
	
	/*static void preProcessEleIDs(Element proc)throws Exception{
		// 1. replace nevis/chronicleid with nevisid, which is the xml fragment id
		refID2DocumentID(proc);
		
		//fully qualify element ids by prepending xml fragment id
		NodeList lst = XMLUtil.xpathNodeSet(proc, "/.", "/servinfosub", "/diagnostic[@IE-ID]");
		for(int i=0; i<lst.getLength(); i++){
			Element ele = (Element)lst.item(i);
			String id = ele.getAttribute("IE-ID") + IDJOIN + ele.getAttribute("id");
			ele.setAttribute("id", id);
		}
		
		//we added its parent servinfosub id as its id temporally
	    //to simplify the coding (its id is a duplicate id as its parent id, we will remove them later)
		lst = XMLUtil.xpathNodeSet(proc, "//diagnostic[not(@IE-ID)]");
		for(int i=0; i<lst.getLength(); i++){
			Element ele = (Element)lst.item(i);
			//its parent should be a <servinfosub>
			String id = ((Element)ele.getParentNode()).getAttribute("id");
			String IE_ID = ((Element)ele.getParentNode()).getAttribute("IE-ID");
			ele.setAttribute("id", id);
			ele.setAttribute("IE-ID", IE_ID);
		}
		
		//few <resact-grp> has id, i.e <resact-grp id="nev10719300n96">
		NodeList eles = XMLUtil.xpathNodeSet(proc, "//testgrp", "//resact-grp[@id]");
		for(int i=0; i<eles.getLength(); i++){
			Element testgrp = (Element)eles.item(i);
			String diagnosticID = XMLUtil.xpathStr(testgrp, "/ancestor::diagnostic/@id");
			diagnosticID = diagnosticID.substring(0, diagnosticID.lastIndexOf(IDJOIN));
			String qualifiedID =  diagnosticID + IDJOIN +testgrp.getAttribute("id");
			testgrp.setAttribute("id", qualifiedID);
		}
		
		//process xref/ref/href references
		lst = XMLUtil.xpathNodeSet(proc, "//xref");
		for(int i=0; i<lst.getLength(); i++){
			Element ele = (Element)lst.item(i);
			Element intxref = ele.getOwnerDocument().createElement("intxref");
			String id = XMLUtil.xpathStr(ele,"/ref/href/text()").replace("#", IDJOIN);
			String textContent = XMLUtil.xpathStr(ele,"/ref/href/@title");
			intxref.setAttribute("refid", id);
			intxref.setAttribute("dest", "info-obj");
			intxref.setTextContent(textContent);
			ele.getParentNode().replaceChild(intxref, ele);
		}
	}*/
	
	/**
	 * to fully qualify element ids to make all of them unique within a file
	 * 1. to make each xml fragment id unique by adding surfix _<seq> (_1, _2) since the same xml fragment might show up more than once in a article
	 * 2. fully qualify elements, such as <testgrp>; 
	 * 		for each servinfosub/diagnostic, it has no id in the source, we added its parent servinfosub id as its id temporally
	 * 		to simplify the coding (its id is same as its parent servinfosub id, it is not unique, we will remove them later) 
	 * 		i.e    <testgrp id="KC01240373">  ==>   <testgrp id="en-US0900c8af80856468-KC01240373">
	 */
	/*void qualifyEleIDs(Document doc)throws Exception{
		String qualifiedID;
		
		NodeList servinfosubs = doc.getElementsByTagName("servinfosub");
		for(int i=0; i<servinfosubs.getLength(); i++){
			Element servinfosub = (Element)servinfosubs.item(i);
			qualifiedID =  servinfosub.getAttribute("IE-ID")+ IDJOIN +servinfosub.getAttribute("id");
			servinfosub.setAttribute("id", qualifiedID);
		}
		
		NodeList diagnostics = doc.getElementsByTagName("diagnostic");
		for(int i=0; i<diagnostics.getLength(); i++){
			Element diagnostic = (Element)diagnostics.item(i);
			if(diagnostic.hasAttribute("id")){
				qualifiedID =  diagnostic.getAttribute("IE-ID")+ IDJOIN +diagnostic.getAttribute("id");
			}else{
				//get its parent <servinfosub> id as its id
				qualifiedID = XMLUtil.xpathStr(diagnostic, "/../@id");
			}
			diagnostic.setAttribute("id", qualifiedID);
		}
		
		NodeList testgrps = doc.getElementsByTagName("testgrp");
		for(int i=0; i<testgrps.getLength(); i++){
			Element testgrp = (Element)testgrps.item(i);
			String diagnosticID = XMLUtil.xpathStr(testgrp, "/ancestor::diagnostic/@id");
			diagnosticID = diagnosticID.substring(0, diagnosticID.lastIndexOf(IDJOIN));
			qualifiedID =  diagnosticID + IDJOIN +testgrp.getAttribute("id");
			testgrp.setAttribute("id", qualifiedID);
		}
	}
	*/
	
	
	/**
	 * return XML fragment id given a node
	 */
	String getRootFragID(Node node) throws Exception{
		pl(XMLUtil.xmlToStringNoHeader(node));
		Element fragRoot = (Element)XMLUtil.xpathNode(node, "/ancestor::diagnostic");
		if(fragRoot==null || !fragRoot.hasAttribute("IE-ID")){
			//should always be servinfosub
			NodeList lst = XMLUtil.xpathNodeSet(node, "/ancestor::servinfosub");
			if(lst.getLength()==1){
				fragRoot = (Element)lst.item(0);
			}else if(lst.getLength()==2){
				fragRoot = (Element)XMLUtil.xpathNode(node, "/ancestor::servinfosub[not(descendant::servinfosub)]");
			}else{
				throw new Exception("number of servinfosub = " + lst.getLength());
			}
		}
		return fragRoot.getAttribute("IE-ID");
	}
	

	static void conntect2DB()throws Exception{
		if(!isOnline){
			pl("not online, skip Connecting to DB");
			con = null;
			return ;
		}
		if(con == null){
			pl("Connecting to DB");
			  con = SQLUtil.connectSQL(AppProperties.prop.getProperty("volvo.db.connectionStr"), AppProperties.prop.getProperty("volvo.db.user"), 
					  AppProperties.prop.getProperty("volvo.db.pass"));
		}
	}
	
	/**
	 * step 1: get the document object of a fragment with id=fragID.
	 * append each childFrag content as a child element to this document. 
	 * if childFragIDs=null, return document for fragID <br>
	 * 
	 * step 2: turn all IDs and references into fully-qualified IDs
	 * 
	 */
	static Document getXMLFragments(String fragID, List<String> childFragIDs) throws Exception {
		pl("get XML fragment fragID="+fragID);

		fragID = fragID.replace("en-US", "");
		Document doc = null;
    	if(!fragCache.containsKey(fragID)){
    		String fragFile = XMLLibDir + Util.getThreeLayerDir(fragID) + fragID + ".xml";
    		//pl("  reading fragment from file system: "+fragID);
   			
			try{
				doc = XMLUtil.parseFile(fragFile);
				fragCache.put(fragID, doc);
			}catch(Exception e){
				//TODO how to process missing XMLs? 
				el("Missing xml frag:" + fragFile);
				return null;
			}
    		
    	}
   		doc = (Document)fragCache.get(fragID).cloneNode(true);    
   		
   		
//NodeList refs = XMLUtil.xpathNodeSet(doc, "//*[ref]");
//for(int i=0; i<refs.getLength();i++){
//	pl("ref parent: " + refs.item(i).getNodeName() + "  XML=" + XMLUtil.xmlToString(refs.item(i)));
//}

		preProcessEleIDs(doc.getDocumentElement());
		if(childFragIDs != null){
			for(int i=0; i<childFragIDs.size(); i++){
				Document childDoc = getXMLFragments(childFragIDs.get(i), null);
				if(childDoc != null){
					Element childContent = (Element)doc.importNode(childDoc.getDocumentElement(), true);
					doc.getDocumentElement().appendChild(childContent);
					//pl("append frag="+childFragIDs.get(i));
				}

			}
		}
		
		return doc;
	}
	
	static Document getRawXMLFragments(String fragID) throws Exception {
		pl("fragID1="+fragID);
		fragID = fragID.replace("en-US", "");
    	String fragFile = XMLLibDir + Util.getThreeLayerDir(fragID) + fragID + ".xml";
		Document doc = XMLUtil.parseFile(fragFile);
		return doc;
	}
	
	/**
	 * to collect all special tools in the Vovlo database and trun them into special tool articles, all model articles will refer to these articles for special tools
	 * This only need to run once for all Vovols models and years
	 */
	void collectSpecialToolsFrags() throws Exception {
		System.out.println("Start of collecting special tool fragment");
		String fileName = SPECIAL_TOOL_FILE + ".xml";

		//Step 1: get TOC
		conntect2DB();
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery("select VV.getAllSpecialToolsXML()");
		rs.next();
		String specialTools  = rs.getString(1);
		//FileUtil.writer(splitTocDir + fileName, specialTools);
		Document doc = XMLUtil.parseStr(specialTools);
		
		//Step 2: insert fragments
		NodeList lst = XMLUtil.xpathNodeSet(doc, "//nevisid");
		for(int i=0; i<lst.getLength(); i++){
			Element ele = (Element)lst.item(i);
			String xmlFragId = ele.getAttribute("id");
			Document xmlFragDoc = getXMLFragments(xmlFragId, null);
			Node frag = doc.importNode(xmlFragDoc.getDocumentElement(), true);
			ele.getParentNode().replaceChild(frag, ele);
		}
		FileUtil.writer(fragInsertedDir + fileName , XMLUtil.xmlToString(doc));

		//Step 3: transform
		System.out.println("Start of tranform and insert graphic info on file "+fileName);
		String sourceID = fragInsertedDir + fileName;
		String xslID = scriptDir + "Volvo_base.xsl";
		String destID = tranformedANDInsertedGXInfoDir + fileName;
		XSLTUtil.transformer(sourceID, xslID, destID, "BaseID", "0");
		
		//insert into graphic info
		Document sdoc = XMLUtil.parseFile(destID);
		insertGXInfo(sdoc);
		FileUtil.writer(this.refSolvedDir + fileName, XMLUtil.xmlToString(sdoc));
	
		//Document docu = XMLUtil.parseFile(destID);
		//FileUtil.writer(destID, XMLUtil.xmlToString(docu));
		this.toSGML();
	}
	
	/**
	 * insert servcat, dochead and docsubhead info into each article xml file
	 *<serv-cat servcat.name="transfer case"/>
	 *<doc-head>2008 TRANSMISSION</doc-head>
	 *<doc-subhead>UF1AE Transfer System - GS350</doc-subhead>
	 */
	private void insertHeadInfo() throws Exception {
		System.out.println("Start inserting servcat, dochead and docsubhead info into each article xml file");
		// load sys, subsys ==> servcat, dochead and docsubhead mapping from spreedsheet
		Map<String, String[]> headMap = loadHeadMapping();
		List<String> files = FileUtil.getAllFilesWithCertainExt(splitTocDir, "xml");
		for (String file : files) {
			//skip orphan articles
			if(file.contains("-Orp-") || file.contains("DTCIndex")){
				continue;
			}
			pl("Adding header info for file "+ file);
			Document doc = XMLUtil.parseFile(splitTocDir + file);
			Element funtionGroup2Node = (Element)XMLUtil.xpathNode(doc, "/FunctionGroup2");
			String funtionGroup2 = funtionGroup2Node.getAttribute("group");
			String Qualifier = XMLUtil.xpathStr(doc, "/FunctionGroup2/@Qualifier");
			String[] headinfo = headMap.get(funtionGroup2);
			
			Element docsubHead = (Element)doc.createElement("doc-subhead");
			//docsubHead.setTextContent(headinfo[1] + " - " + Qualifier + " (544 BODY)");
			docsubHead.setTextContent(headinfo[1] + " - " + Qualifier );
			funtionGroup2Node.insertBefore(docsubHead, funtionGroup2Node.getFirstChild());
			
			Element docHead = (Element)doc.createElement("doc-head");
			docHead.setTextContent(headinfo[0]);
			funtionGroup2Node.insertBefore(docHead, funtionGroup2Node.getFirstChild());

			String[] servcats = headinfo[2].split(";");
			for(int i=0; i<servcats.length; i++){
				Element servcat = (Element)doc.createElement("serv-cat");
				servcat.setAttribute("servcat.name", servcats[i]);
				funtionGroup2Node.insertBefore(servcat, funtionGroup2Node.getFirstChild());
			}
			FileUtil.writer(splitTocDir + file, XMLUtil.xmlToString(doc));
		}
		System.out.println("End of inserting servcat, dochead and docsubhead info");
	}
	
	/**
	 * load functionGroup2 id ==> servcat, dochead and docsubhead mapping from spreedsheet 
	 */
	private Map<String, String[]> loadHeadMapping() throws Exception {
		System.out.println("Start loading head mapping info");
		Map<String, String[]> headMap = new HashMap<String, String[]>();
		InputStream inp = new FileInputStream(scriptDir + "Volvo.xls");
		HSSFWorkbook wb = new HSSFWorkbook(new POIFSFileSystem(inp));
		HSSFSheet sheet = wb.getSheet("Head Info");
		int rows = sheet.getLastRowNum();
		//skip row 0, which is the head
		for(int i=1; i<=rows; i++){
			Row row = sheet.getRow(i);
			String FG2_ID , dochead, docsubhead, serv_catsStr;
			if(row.getCell(0) != null){
				FG2_ID = row.getCell(0).getStringCellValue();
				dochead = row.getCell(1).getStringCellValue().trim();
				docsubhead = row.getCell(2).getStringCellValue().trim();
				serv_catsStr = row.getCell(3).getStringCellValue().trim();
				String[] headerinfo = {dochead, docsubhead, serv_catsStr};
				//pl("headerinfo="+FG2_ID+" "+dochead+ " " + docsubhead+ " " + serv_catsStr);
				headMap.put(FG2_ID, headerinfo);
			}
		}
		System.out.println("End of loading head mapping info");
		return headMap;
	}
	
	/**
	 * Export all graphics (CGM, JPEG, GIF, JPG, SVG) from database to Library 
	 */
	public static void exportGXs2Lib()throws Exception{
		pl("Start of exporting all graphics to Lib "+new Date());
	  conntect2DB();
	  Statement stmt = con.createStatement();
	  String query = "insert into [Conversion].[dbo].[vv_graphicMap] (fk_path) " + 
			  "SELECT b.path FROM [Conversion].[dbo].[vv_graphicMap] a  right join [Volvo_ImageRepository].[dbo].[LocalizedGraphics] b " + 
			  "on a.fk_path = b.path where a.fk_path is null";
	  int co = stmt.executeUpdate(query);
	  pl("Number of new graphics inserted into vv_graphicMap table:"+co);

	  String[] gxTypes = {"CGM", "JPEG", "GIF", "JPG", "SVG"};
	  for(int i=0; i<gxTypes.length; i++){
		  pl("Start of exporting " + gxTypes[i]  + " "+new Date());
		  query = "select d.id, a.imageData from Volvo_imageRepository.dbo.LocalizedGraphics a " +
					"join [Volvo_ImageRepository].[dbo].[Graphics] b on a.fkGraphic = b.id " +
					"join [Volvo_ImageRepository].[dbo].[GraphicFormats] c on b.fkGraphicFormat = c.id " +
					"join [Conversion].[dbo].[vv_graphicMap] d on a.path = d.fk_path " +
					"where c.description = '" + gxTypes[i] + "'";
			pl("query="+query);
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next())
	       {	
				//MEPS only accept .jpg files, no .jpeg 
				String fileName = "LL" + rs.getString(1) + "." + gxTypes[i].replace("JPEG", "JPG").toLowerCase();
				byte[] fileBytes = rs.getBytes(2);
				OutputStream targetFile=  new FileOutputStream( HomeDir + "lib\\" + gxTypes[i] + "\\" + fileName );
				pl("write file "+fileName);
				 targetFile.write(fileBytes);
	             targetFile.close();
	       }
			pl("End of exporting " + gxTypes[i]  + " "+new Date());
	  }
	  pl("End of exporting all graphics to Lib; count="+count+"  "+new Date());
	}
	
	//  select * from  [dbo].[S_DocumentContent] f where f.fkDocumentID = '-2057614095' and fkLanguage = 15;
	/**
	 * New method for 2017 data  
	 */
	public static void exportXML()throws Exception{
		pl("Start of exporting all xml fragments  "+new Date());
		String dir = HomeDir + "temp\\";
		conntect2DB();
		Statement stmt = con.createStatement();
		
		ResultSet rs;
		String query = "select f.fkDocumentID, f.xmlContent from  [dbo].[S_DocumentContent] f "
				+ "where f.fkDocumentID in ('-2057614095','-2057431141','-2026546539','-2138575740','-2035564871','-2122455834',"
				+ "'-2055798082','-2142033894','-2056524469','-2138612058','-2142033557','-2142031854','-2088247289','-2142335331',"
				+ "'-2142323464','-2106281902','-2138579874','-2137908546','-2146134820','-2122455682','-2106281009','-2142348077','-2138579937',"
				+ "'-2142031844','-2142121651','-2146379373','-2076609518','-2087014222','-2080714861','-2138575818','-2138611779','-2137972765',"
				+ "'-2142031644','-2142032269','-2142323811','-2138612055','-2146363800','-2122455986','-2146191367','-2138579935','-2134596638',"
				+ "'-2137972454','-2138601478','-2146665532','-2105155488','-2138579938','-2138614387','-2142033876','-2138575837','-2080714903','-2137804651',"
				+ "'-2080714873','-2057218884','-2064398313','-2099668433','-2120427704','-2138601477','-2134998186','-2138575760','-2122455633','-2146608807',"
				+ "'-2142033826','-2142602859','-2058992771','-2142033929','-2142153651','-2142033582','-2142033801','-2146379441', '-2075292874', '-2026546556' ) and fkLanguage = 15;" ;
		rs = stmt.executeQuery(query);
		
		int count = 0;
		while (rs.next())
	    {	
			String fileName = "Document" + rs.getString(1);
			byte[] fileBytes = rs.getBytes(2);
			if(fileBytes == null){
				//0900c8af836d762b has no xmlContent
				continue;
			}

			String fileFullName = dir + fileName  + ".xml";
			OutputStream targetFile=  new FileOutputStream(fileFullName);

			pl("write file "+fileFullName  );
			targetFile.write(fileBytes);
	        targetFile.close();
	    }  
		pl("End of exporting all xml fragments to Lib; count="+count+"  "+new Date());
	}
	
	/**
	 * Export all xml fragments from database to Library 
	 */
	public static void exportXMLFrags2Lib()throws Exception{
		pl("Start of exporting all xml fragments to Lib "+new Date());
		String xmlFragLibDir = HomeDir + "lib\\xml\\";
		conntect2DB();
		Statement stmt = con.createStatement();
		ResultSet rs;
		String query = "select a.nevisId, e.XMLContent from dbo.[S_Document] a join  [dbo].[S_DocumentContent] e on a.id = e.fkDocumentID where e.fkLanguage = 15 order by a.nevisid " ;
		 // + "where nevisid in ('0900c8af8338b523', '0900c8af844563cf')";
		rs = stmt.executeQuery(query);
		int count = 0;
		while (rs.next())
	    {	
			pl("Retriving xml from db: nevisId=" + rs.getString(1));
			String fileName = rs.getString(1);
			byte[] fileBytes = rs.getBytes(2);
			
			if(fileBytes == null){
				//0900c8af836d762b has no xmlContent
				pl("Skipping emtpy xml: nevisId=" + rs.getString(1));
				continue;
			}

			count++;
			String subDir = Util.getThreeLayerDir(fileName);
			String dir = xmlFragLibDir+subDir;
	        	
			File DIR = new File(dir); 
			if(!DIR.exists()){
				if(!DIR.getParentFile().exists()){
					if(!DIR.getParentFile().getParentFile().exists()){
						DIR.getParentFile().getParentFile().mkdir();
					}
					DIR.getParentFile().mkdir();
				}
				new File(dir).mkdir();
			}
			OutputStream targetFile=  new FileOutputStream(dir + fileName  + ".xml");
			pl("write file "+ dir + fileName  + ".xml");
			targetFile.write(fileBytes);
	        targetFile.close();
//			Unzip.unzipFile(dir + fileName  + ".zip", dir , true);        
//	        new File(dir + fileName  + ".zip").delete();
	        if((count % 1000)==0){
	        	pl("== exporting xml frags, count="+count);
	        }
	    }  
		pl("End of exporting all xml fragments to Lib; count="+count+"  "+new Date());
		
		
//		pl("Start of renaming all xml fragments");
//		//rename all xml fragments (0800c8af80a2e92c_en-US.xml => 0800c8af80a2e92c.xml) 
//		List<String> files = FileUtil.getAllFilesWithCertainExt(xmlFragLibDir, "xml", true);
//		for(int i=0; i<files.size(); i++){
//			String fileName = files.get(i).substring(files.get(i).lastIndexOf("\\")+1);
//			String dir = files.get(i).replace(fileName, "");
//			String newFileName = fileName.substring(0, fileName.indexOf("_"));
//			pl("fileName="+fileName+ " newFileName="+newFileName);
//			new File(files.get(i)).renameTo(new File(dir+newFileName+".xml"));
//		}
//		pl("End of renaming all xml fragments");
	}
	
	/**
	 * return all Lexus article-level XML nodes from toc.xml document object
	 */
	private static List<Node> getArticleLevelEle(Document doc) throws Exception {
		List<Node> lst = new ArrayList<Node>();
		//String xpath = "//tocxml/tocitem/tocitem";
		String xpath = "//tocxml/tocitem/tocitem";
		//get a list of top-level tocitems
		NodeList parentTOCItems = (NodeList)XMLUtil.xpathNodeSet(doc, xpath);
		for(int i=0; i<parentTOCItems.getLength(); i++){
			Element parentTOCItem = (Element)parentTOCItems.item(i);
			//get tocitem name from the <tocname> element
			String perentTOCName = XMLUtil.getDirectChildElementsByTagName(parentTOCItem, "tocname").get(0).getTextContent();
			int sysNum = i;
			List<Element> tocItems = XMLUtil.getDirectChildElementsByTagName(parentTOCItem,  "tocitem");
			for(Element tocItem:tocItems){
				String tocName = XMLUtil.getDirectChildElementsByTagName(tocItem, "tocname").get(0).getTextContent();
				List<Element> subTOCItems = XMLUtil.getDirectChildElementsByTagName(tocItem,  "tocitem");
				//these systems need to be further split
				if((perentTOCName.equalsIgnoreCase("PREPARATION") && tocName.contains("PREPARATION")) ||
						(perentTOCName.contains("SPECIFICATIONS") && tocName.contains("SPECIFICATIONS"))){
					for(Element subTOCItem:subTOCItems){
						Element newTOCItem = (Element)tocItem.cloneNode(true);
						doc.importNode(newTOCItem, true);
						tocItem.getParentNode().insertBefore(newTOCItem, tocItem);
						XMLUtil.xpathRemove(newTOCItem, "/tocitem");
						//doc.importNode(newTOCItem, true);
						newTOCItem.appendChild(subTOCItem);
						String subTOCItemName = XMLUtil.getDirectChildElementsByTagName(subTOCItem, "tocname").get(0).getTextContent();
						//inject sysnum and sysid (parent.tocName + "," + child.tocName) 
						Node comment = doc.createComment(sysNum + "," + perentTOCName + "," + tocName + "," + subTOCItemName);
						newTOCItem.insertBefore(comment, newTOCItem.getFirstChild());
						lst.add(newTOCItem);
					}
					tocItem.getParentNode().removeChild(tocItem);
				}else if(perentTOCName.equalsIgnoreCase("INTRODUCTION") && tocName.contains("INTRODUCTION")){
					Node comment = doc.createComment(sysNum + "," + perentTOCName + "," + tocName );
					tocItem.insertBefore(comment, tocItem.getFirstChild());
					lst.add(tocItem);
				}
				else{//toname contains 'system' need put into a separate article
					for(Element subTOCItem:subTOCItems){
						String subTOCItemName = XMLUtil.getDirectChildElementsByTagName(subTOCItem, "tocname").get(0).getTextContent();
//pl(subTOCItemName);
						if(subTOCItemName.toLowerCase().contains("system")){
							Element newTOCItem = (Element)tocItem.cloneNode(true);
							doc.importNode(newTOCItem, true);
							tocItem.getParentNode().insertBefore(newTOCItem, tocItem);
							XMLUtil.xpathRemove(newTOCItem, "/tocitem");
							newTOCItem.appendChild(subTOCItem);
							
							//inject sysnum and sysid (parent.tocName + "," + child.tocName) 
							Node comment = doc.createComment(sysNum + "," + perentTOCName + "," + tocName + "," + subTOCItemName);
							newTOCItem.insertBefore(comment, newTOCItem.getFirstChild());
							List<Node> articles = splitSystemArticle(newTOCItem);
							lst.addAll(articles);
						}
					}
					if(XMLUtil.xpathNodeSet(tocItem, "//tocitem[@file_ref]").getLength() > 0){
						Node comment = doc.createComment(sysNum + "," + perentTOCName + "," + tocName );
						tocItem.insertBefore(comment, tocItem.getFirstChild());
						lst.add(tocItem);
					}
				}
			}
		}
		System.out.println("End of get article Level XMLs, number of articles = "+lst.size());
		return lst;
	}
	
	//need to split system articles into two parts, one part with all DTC and testing info, the other part contains the rest info
	private static List<Node> splitSystemArticle(Node n) throws Exception {
		List<Node> articles = new ArrayList<Node>();
		int dtcCharts = XMLUtil.xpathNodeSet(n, "//tocitem[@file_ref and contains(tocname/text(), 'DIAGNOSTIC TROUBLE CODE')]").getLength();
		if(dtcCharts >= 1){
			Element newTOCItem = (Element)n.cloneNode(true);
			n.getOwnerDocument().importNode(newTOCItem, true);
			n.getParentNode().insertBefore(newTOCItem, n);
			XMLUtil.xpathRemove(n, "//tocitem[@file_ref and contains(tocname/text(), 'DIAGNOSTIC TROUBLE CODE')][" + dtcCharts + "]/following-sibling::tocitem[@file_ref]");
			XMLUtil.xpathRemove(newTOCItem, "//tocitem[@file_ref and contains(tocname/text(), 'DIAGNOSTIC TROUBLE CODE')][1]/preceding-sibling::tocitem[@file_ref]");
			XMLUtil.xpathRemove(newTOCItem, "//tocitem[@file_ref and contains(tocname/text(), 'DIAGNOSTIC TROUBLE CODE')]");
			articles.add(n);
			articles.add(newTOCItem);
		}else {
			articles.add(n);
		}
		return articles;
	}
/*
	//need to further to split over-sized article level XMLs
	private static List<Node> splitArticleXML(Node n) throws Exception {
		List<Node> articles = new ArrayList<Node>();
		String tocname = XMLUtil.getDirectChildElementsByTagName(n.getParentNode(), "tocname")
				.get(0).getTextContent();
		if (tocname.equalsIgnoreCase("TRANSMISSION")
				|| tocname.equalsIgnoreCase("ENGINE")
				|| tocname.equalsIgnoreCase("BRAKE")
				|| tocname.equalsIgnoreCase("COMMUNICATION SYSTEM")
				|| tocname.equalsIgnoreCase("STEERING")
				|| tocname.equalsIgnoreCase("RESTRAINTS")
				|| tocname.equalsIgnoreCase("BODY ELECTRICAL")) {
			System.out.println("Start of splitting article " + tocname);
			// <tocitem sourcegi="repair_procedure"
			// file_ref="RM000000PD8041X.xml">
			NodeList fileRefs = XMLUtil.xpathNodeSet(n, "//tocitem[@file_ref]");
			// System.out.println(fileRefs.getLength() + " file_refs found");
			for (int i = 0; i < fileRefs.getLength(); i++) {
				String xpath = XMLUtil.getXpath(fileRefs.item(i));
				Element nn = (Element) (XMLUtil.xpathNodeSet(n.getOwnerDocument(),
						xpath).item(0));
				nn.setAttribute("article", i + "");
			}
			// System.out.println("n22="+XMLUtil.xmlToString(n));

			int fileRefCount = fileRefs.getLength();
			double numOfArticles = (fileRefCount + .01)
					/ MAX_XMLFRAG_PER_ARTICLE;
			long filesPerArtilce = Math.round((numOfArticles / (Math
					.round(numOfArticles + .5))) * MAX_XMLFRAG_PER_ARTICLE) + 1;
			// System.out.println("n1="+XMLUtil.xmlToString(n));
			for (int i = 0; i < Math.ceil(numOfArticles); i++) {
				long start = i * filesPerArtilce;
				long end = (i + 1) * filesPerArtilce;
				Node newNode = n.cloneNode(true);
				n.getOwnerDocument().importNode(newNode, true);
				n.getParentNode().insertBefore(newNode, n);
				// System.out.println("newNode.getOwnerDocument()1="+XMLUtil.xmlToString(newNode.getOwnerDocument()));
				for (int j = 0; j < fileRefs.getLength(); j++) {
					if (j < start || j >= end) {
						String xp = "//tocitem[@article='" + j + "']";
						int c = XMLUtil.xpathRemove(newNode, xp);
						//System.out.println("xp=" + xp + "; c=" + c);
						//remove all <tocitem>s without <tocitem file_ref=...> in it
						xp = "//tocitem[not(@file_ref) and count(descendant::tocitem[@file_ref])=0]";
						c = XMLUtil.xpathRemove(newNode, xp);
						//System.out.println("xp=" + xp + "; c=" + c);
					}
				}
				//System.out.println("newNode=" + XMLUtil.xmlToString(newNode));
				articles.add(newNode);
			}
			// System.out.println("n2="+XMLUtil.xmlToString(n));
			System.out.println("End of splitting article XML");
		} else {
			articles.add(n);
		}
		return articles;
	}
*/
	/**
	 * remove tocitems which don't belong to this XML file
	 *  <tocitem sourcegi="repair_procedure" file_ref="RM000000UYV04SX.xml">
                 <tocname>INTRODUCTION: HOW TO USE THIS MANUAL: GENERAL INFORMATION (2008 GS350 GS460)</tocname>
        </tocitem> 
	 */
	/*private static void cleanToc4Model(Element node, String model)throws Exception{
		NodeList lst = node.getElementsByTagName("tocitem");
		List<Node> removeNodes = new ArrayList<Node>();
		//identify tocitems which don't belong to this model
		for(int i=0; i<lst.getLength(); i++){
			Element tocitem = (Element)lst.item(i);
			if(tocitem.getAttribute("sourcegi").equalsIgnoreCase("repair_procedure") &&
					!tocitem.getAttribute("file_ref").equals("")){
				String tocname = XMLUtil.getDirectChildElementsByTagName(tocitem, "tocname").get(0).getTextContent();
				if(!tocname.contains(model)){
					//INTRODUCTION: HOW TO USE THIS MANUAL: GENERAL INFORMATION (2008 GS350 GS460)
					removeNodes.add(tocitem);
				}
			}
		}
		//remove <tocitem file_ref...>s which don't belong to this model
		for(Node n: removeNodes){
			n.getParentNode().removeChild(n);
		}
		
		//check if a tocitem contains any <tocitem sourcegi="repair_procedure" file_ref="XXXXXXX.xml">, if it doesn't, delete this tocitem
		lst = node.getElementsByTagName("tocitem");
		removeNodes.clear();
		for(int i=0; i<lst.getLength(); i++){
			Element ele = (Element)lst.item(i);
			//if tocitem itself contains file_ref attribute, keep it
			if(!ele.getAttribute("file_ref").equals("")){
				continue;
			}
			//use xpath to check if it has any children with file_ref attribute
			//System.out.println("node==" + XMLUtil.xmlToString(node));
			//System.out.println("ele==" + XMLUtil.xmlToString(ele));
			//System.out.println("ele.parent==" + XMLUtil.xmlToString(ele.getParentNode()));
			//System.out.println("ele.parent==" + XMLUtil.xmlToString(ele.getOwnerDocument()));
			NodeList file_refs = XMLUtil.xpathNodeSet(ele, "//*[@file_ref]");
			//System.out.println(file_refs.getLength());
			//no child tocitem with file_ref attribute exist  
			if(file_refs.getLength() == 0){
				if(model.equals("GS460")){
					//System.out.println("Removed tocitem--" + XMLUtil.xmlToString(ele));
				}
				removeNodes.add(ele);
			}
		}
		//remove tocitem nodes which has no file_ref tocitem nodes in it
		for(Node n: removeNodes){
			if(n!=null)
				n.getParentNode().removeChild(n);
		}
		
		//FileUtil.writer(fileName, XMLUtil.xmlToString(node));
	}*/
	
	/**
	 * Collect all elements (element with id attribute, <xref linkend ) parent-child relationship 
	 * by peeling them layer by layer, one layer at a time.
	 * <repair_procedure id="RM000000WZ105YX" category="J" from="200709" xml:lang="en">
	 *  
	 *  <xref linkend="RM000000WZ105YX_04_0006" 
	 */
	
	private static void collectChildParentRelations(Node node, String parentKey) {
		if (node.getNodeType() == Node.DOCUMENT_NODE) {
			collectChildParentRelations(((Document) node).getDocumentElement(),
					parentKey);
		} else if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element ele = (Element) node;
			String eleName = node.getNodeName();
			String parent = parentKey;
			String id ;
			if(!eleName.equals("xref")){
				id = ele.getAttribute("id");
				//id = ele.getAttribute("file_ref");
				if (id != null && id.trim().length() > 0) {
					// there should be NO duplicate child-parent relations
					count++;
					String[] pair = {id, parent};
					mapList.add(pair);
					//if(parent == null) System.out.println(id + "--"+ parent);
					parent = id;
				}
				NodeList nl = ele.getChildNodes();
				for (int i = 0; i < nl.getLength(); i++) {
					collectChildParentRelations(nl.item(i), parent);
				}
			}else{//it is <xref element , according to publication.dtd, it should have no sub elements
				id = ele.getAttribute("linkend");
				count++;
				String[] pair = {id.trim(), parentKey};
				mapList.add(pair);
			}
		}
	}
	
	/**
	 * extrac all graphic names from a model XML files
	 */
	private static List<String> getAllGraphicNames() throws Exception{
		System.out.println("Start collecting all graphic names from XMLs");
		List<String> graphicNames = new ArrayList<String>();
		//List<String> files = FileUtil.getAllFilesWithCertainExt(inDir, "xml");
		List<String> files = FileUtil.getAllFilesWithCertainExt(ModelOutDir, "xml");
		for(String file:files){
			System.out.println(file);
			Document doc = XMLUtil.parseFile(ModelOutDir + file);
			NodeList lst = XMLUtil.xpathNodeSet(doc, "//TMS-media_object");
			for(int i=0; i<lst.getLength(); i++){
				String graphic = ((Element)lst.item(i)).getAttribute("url");
				if(!graphicNames.contains(graphic)){
					graphicNames.add(graphic);
				}
			}
		}
		System.out.println(graphicNames.size() + " graphics collected");
		for(String s:graphicNames){
			System.out.println(s);
		}
		return graphicNames;
	}
	
	
	/**
	 * There are some info we don't need from the toc.xml, let's get rid of them before we split it
	 * There are a couple of toc.xml in a different format (such as 09IS-F), let's convert it to standard format
	 */
	private static Document trimToc(String tocFile) throws Exception{
		System.out.println("Start trimming toc.xml");
		int count = 0;
		Document doc = XMLUtil.parseFile(tocFile);
		Node generalTocitem = XMLUtil.xpathNode(doc, "/tocxml/tocitem[1]/tocitem[lower-case(tocname/text())='general']");
		//special format
		if(generalTocitem != null){
			pl("this toc.xml comes with the special format");
			NodeList tocitems = XMLUtil.xpathNodeSet(generalTocitem, "/tocitem");
			if(tocitems.getLength() != 4){
				throw new Exception("unknown toc.xml format found");
			}
			count += XMLUtil.xpathRemove(doc, "/tocxml/tocitem[1]/tocitem[tocname='INTRODUCTION']");
			for(int i=0; i<tocitems.getLength(); i++){
				Element tocitem = (Element)tocitems.item(i);
				Node tocName = doc.createElement("tocname");
				tocName.setTextContent(XMLUtil.xpathStr(tocitem, "/tocname/text()"));
				
				Element ele = doc.createElement("tocitem");
				ele.setAttribute("sourcegi", "service_category");
				ele.appendChild(tocName);
				ele.appendChild(tocitem);
				generalTocitem.getParentNode().insertBefore(ele, generalTocitem);
			}
			generalTocitem.getParentNode().removeChild(generalTocitem);
			//pl(" Special format fixed=="+XMLUtil.xmlToString(doc));
		}else{//standard format
			count += XMLUtil.xpathRemove(doc, "/tocxml/tocitem[1]/tocitem[tocname='INTRODUCTION']/tocitem[tocname!='INTRODUCTION']");
		}
		System.out.println("End trimming toc.xml "+ count + " elements trimmed");
		return doc;
	}

	
	/**
	 * to collect all gif graphics info for all models from unzipped Lexus data 
	 */
	/* Map<String, Map<String, String>> collectGXInfo(String graphicFileExt, String folder ) throws Exception{
		System.out.println("Starting to collect all graphic info into oenames HashMap from " + folder);
		List<String> gifs = FileUtil.getAllFilesWithCertainExt(folder,graphicFileExt, true );
		System.out.println(gifs.size() + " GIFs collected");
		Map<String, String> modelOE ;
		String fileName, model, mark, oename;
		for(int i=0; i<gifs.size(); i++){
			fileName = gifs.get(i).substring(gifs.get(i).lastIndexOf("\\")+1);
			//E050367
			oename = fileName.replace("." + graphicFileExt, "");
			mark = "\\" + year;
			//get:  ES350_RM10K0U_EN_10-03-19_UB\graphics\gif\E050367.gif
			model = gifs.get(i).substring(gifs.get(i).indexOf(mark) + mark.length()) ;
			model = model.substring(0, model.indexOf("_"));
			
			//insert into oenames map
			if(OENAMES.containsKey(oename)){
				OENAMES.get(oename).put(model, gifs.get(i));
			}else{
				modelOE = new HashMap<String, String>();
				modelOE.put(model, gifs.get(i));
				OENAMES.put(oename, modelOE);
			}
		}
//System.out.println("OENAMES=\n" + oenames2String());
		System.out.println(OENAMES.size() + " oename info collected");
		System.out.println("End of collecting all graphic info from " + folder);
		return OENAMES;
	}*/
	
	/**
	 * given a batch_id, load oe_name to generated_id mapping from LX_OE table.
	 * loadType = "ALL" - load all mapping
	 * loadType = "NEW" - load all mapping with LX_OE.NEW_OE = "TRUE"  
	 * loadType = "OLD" - load all mapping with LX_OE.NEW_OE = "FALSE"
	 */
	static Map<String, String> loadOE2GeneratedIDMapping(int batch_id, String loadType) throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		System.out.println("Start of loading oe_name to generated_id mapping from LX_OE table.");
		Statement st = meps.con.createStatement();
		String query;
		if(loadType.equalsIgnoreCase("ALL")){
			query = "select oe_name, generated_id from LX_OE where batch_id=" + batch_id;	
		}else if(loadType.equalsIgnoreCase("NEW")){
			query = "select oe_name, generated_id from LX_OE where new_oe = 'TRUE' and batch_id=" + batch_id;
		}else if(loadType.equalsIgnoreCase("OLD")){
			query = "select oe_name, generated_id from LX_OE where new_oe = 'FALSE' and batch_id=" + batch_id;
		}else{
			throw new Exception("Wrong loadType="+loadType);
		}
		ResultSet rs = st.executeQuery(query);
		String oe_name, gid;
		while (rs.next()) {
				oe_name = rs.getString(1);
				gid = rs.getString(2);
				map.put(oe_name, gid);
		}
		System.out.println("End of loading oe_name to generated_id mapping from LX_OE table. items loaded="+map.size());
		return map;
	}
	
	/**
	 * load existing <oename, generated_id> mapping
	 */
	static Map<String, String> getOE_Generated_id_mapping() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		System.out.println("Start of loading oe_name, generated_id from LX_OE_GENERATEDID table.");
		Statement st = meps.con.createStatement();
		String query;
		query = "select oe_name, generated_id from LX_OE_GENERATEDID order by oe_name";
		ResultSet rs = st.executeQuery(query);
		String oe_name, gid;
		while (rs.next()) {
				oe_name = rs.getString(1);
				gid = rs.getString(2);
				map.put(oe_name, gid);
				//System.out.println(oe_name + ";"+ gid + Util.DELIMITER + caption);
		}
		System.out.println("End of loading oe_name, generated_id from LX_OE_GENERATEDID table. items loaded="+map.size());
		return map;
	}
	
	/**
	 * given a batch_id, load oe_name, generated_id and caption mapping from LX_OE table.
	 * <oename, concat(generated_id, caption)>
	 * batch_id is optional, enter -1 to retrieve the latest batch_id from lX_OE table
	 */
	static Map<String, String> loadOEInfoFromDB() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		System.out.println("Start of loading oe_name, generated_id from vv_graphicMap table");
		Statement st = con.createStatement();
		String query = "select fk_path, id from [Conversion].[dbo].[vv_graphicMap]";
		ResultSet rs = st.executeQuery(query);
		String oe_name, gid;
		while (rs.next()) {
				oe_name = rs.getString(1);
				gid = "GLL" + rs.getString(2); 
				map.put(oe_name, gid);
		}
		System.out.println("End of loading oe_name, generated_id from vv_graphicMap table items loaded="+map.size());
		return map;
	}
	
	/**
	 * For each new oename in LX_OE table, pick its first <model, fileFullName> from oenames HashMap
		o	Rename them with generatred_id and move them into output/importMEPS/
		o	remove this new oename from oenames HashMap IF it is the only <model, fileFullName> item (we done with them).
	 */
	/*static void processNewGX(int batch_id) throws Exception {
		System.out.println("Start of processing new graphics");
		//Load all new oenames and its corresponding generated_ids from LX_OE table
		Map<String, String> newOEs = loadOE2GeneratedIDMapping(batch_id, "NEW");
		Set<String> oeKeys = newOEs.keySet();
		int count = 0;
		int batch = 1;
		String batchDir = import2MEPSDir;
		for(String s:oeKeys){
			// <ES350, N:\Automation\Toyota\In\in\09ES350_RM10K0U_EN_10-03-19_UB\graphics\gif\E050367.gif>
			Map<String, String> map = OENAMES.get(s);
			//could be any model
			String model = (String)map.keySet().toArray()[0];
			String gid = newOEs.get(s);
			String gifFile = map.get(model);
			int batchSize = 15000;
			if(oeKeys.size()>batchSize){
				if(count++ % batchSize == 0){
					batchDir = import2MEPSDir + "batch" + batch++ + "\\";
					(new File(batchDir)).mkdir();
				}
			}
			FileUtil.copyFile(gifFile, batchDir + gid.substring(1) + ".gif");
			//srcFile.renameTo(destFile);
			if(map.size()==1) {
				//this new graphic has only one instance, we done with it.
				map.put(model, null);
			}
		}
		cleanOENamesMap();
		System.out.println("End of processing new graphics; " + newOEs.size() + "new graphics files moved to " + import2MEPSDir);
	}*/
	
	/**
	 * to remove all <model, fileFullName> which have duplicate file content with its same oename siblings
	 */
	/*static void dedup() throws Exception {
		System.out.println("Start of dedupping oenames map");
		Set<String> oeKeys = OENAMES.keySet();
		int count1 = 0, count2 = 0;
		for(String s:oeKeys){
			//if(!s.equalsIgnoreCase("I000023-A")) continue;
			// <ES350, N:\Automation\Toyota\In\in\09ES350_RM10K0U_EN_10-03-19_UB\graphics\E050367.eps>
			Map<String, String> map = OENAMES.get(s);
			List<String> models = new ArrayList<String>(map.keySet());
			//move all <meps, fileFullPath> to the top so they will not be deleted later
			if(models.contains("meps")){
				models.remove("meps");
				models.add(0, "meps");
				//System.out.println("10-oenames==\n"+oenames2String());
			}
			//compare each gif with all its previous siblings
			for(int i = 1; i < models.size(); i++){
				String model1 = models.get(i);
				String gif1 = map.get(model1);
				//all its previous siblings
				for(int j = 0; j< i; j++){
					String model2 = models.get(j);
					String gif2 = map.get(model2);
					if(gif2 != null){
						if(isSameFile(gif1, gif2)){
							//overwrite existing entry with null as gif file full name
							map.put(model1, null);
							count1++;
							//System.out.println("file " + gif1 + " removed as a duplicate of " + gif2 );
							break;
						}else{
							count2++;
							//System.out.println("file " + gif1 + " is different from file " + gif2 );
						}
					}
				}
				//System.out.println(count1+ " " + count2);
				if((count1+count2) % 500 == 0) System.out.print((count1+count2)+" ");
			}
		}
		cleanOENamesMap();
		//remove oename which has only a <meps, fileFullName> left, these are duplicate existing gif, we donâ€™t need do anything
		oeKeys = OENAMES.keySet();
		int count = 0;
		for(String s:oeKeys){
			Set<String>models = OENAMES.get(s).keySet();
			if(models.size()==1 && models.contains("meps")){
				OENAMES.get(s).put("meps", null);
				count ++;
			}
		}
		cleanOENamesMap();
		System.out.println(count + " MEPS gifs removed from oename HashMap since all new gifs are duplicate of them");
		System.out.println("\n" + count1 + " duplicate gifs removed from map; " + count2 + " different gifs kept" );
	}*/
	
	/**
	 * clean up all oenames map entries
	 * 1. remove all map entry with no sub entries
	 * 2. remove all sub entries with null as full file name
	 */
	/*static int cleanOENamesMap() {
		int count = 0;
		List<String> removeOEs = new ArrayList<String>();
		List<String> removeModels = new ArrayList<String>();
		
		Set<String> oeKeys = OENAMES.keySet();
		for (String s : oeKeys) {
			// <ES350, N:\Automation\Toyota\In\in\09ES350_RM10K0U_EN_10-03-19_UB\graphics\E050367.eps>
			Map<String, String> map = OENAMES.get(s);
			if (map == null || map.size() == 0) {
				count ++;
				removeOEs.add(s);
				continue;
			}
			
			removeModels.clear();
			Set<String> models = map.keySet();
			for (String m : models) {
				String fileFullName = map.get(m);
				if (fileFullName == null) {
					count++;
					removeModels.add(m);
				}
			}
			for(String m:removeModels){
				map.remove(m);
			}
			if (map.size() == 0) {
				count++;
				removeOEs.add(s);
			}
		}
		for(String s:removeOEs){
			OENAMES.remove(s);
		}
		System.out.println(count + " oenames entries or sub entries removed");
		return count;
	}*/
	
	/**
	 * to check if two files have same binary content
	 */
	static boolean isSameFile(String file1, String file2) throws Exception {
		File f1 = new File(file1);
		File f2 = new File(file2);
		if(!f1.exists() || !f2.exists()){
			throw new Exception("File(s) not found; "+ file1 + "  " + file2);
		}
		return FileUtil.compareFiles_Binary(file1, file2);
	}
	
	/**
	 * Caution! don't modify it, we need the same format to reload oenames map from txt file which content is generated by this method
	 * output oenames map as a string,  
	 */
	/*static String oenames2String(){
		String result = "";
		Set<String> oeKeys = OENAMES.keySet();
		for (String s : oeKeys) {
			// <ES350, N:\Automation\Toyota\In\in\09ES350_RM10K0U_EN_10-03-19_UB\graphics\E050367.eps>
			Map<String, String> map = OENAMES.get(s);
			Set<String> models = map.keySet();
			for (String m : models) {
				String fileFullName = map.get(m);
				result += s + "\t" + m + "\t" + fileFullName + "\n";
			}
		}
		return result;
	}*/
	
	/**
	 * reload oenames map from file  
	 * C131384E01 GX470 N:\Automation\Toyota\In\in\09GX470_RM10P0U_EN_10-01-26_UB\graphics\C131384E01.eps
	 * @throws Exception 
	 */
	/*static void reloadOenamesMap(String fileName) throws Exception{
		System.out.println("Reloading oenames map from " + fileName);
		OENAMES.clear();
		BufferedReader in = new BufferedReader(new FileReader(fileName));
		String str, oename, model, fileFullName;
		Map<String, String> map ;
		while ((str = in.readLine()) != null) {
			oename = str.substring(0, str.indexOf("\t")).trim();
			model = str.substring(str.indexOf("\t")+1, str.lastIndexOf("\t")).trim();
			fileFullName = str.substring(str.lastIndexOf("\t")+1).trim();
			if(fileFullName.equalsIgnoreCase("null")){
				fileFullName = null;
			}
			if(OENAMES.containsKey(oename)){
				map = OENAMES.get(oename);
				map.put(model, fileFullName);
			}else{
				map = new HashMap<String, String>();
				map.put(model, fileFullName);
				OENAMES.put(oename, map);
			}
		}
		in.close();
		System.out.println(OENAMES.size() + " oenames loaded into oenames map from " + fileName);	
	}*/

	/**
	 * get all model zips zip files in /rawdata folder, input parameters are a list of source folders, 
	 * same zip files in former folders overwrite zip files in later folders
	 * e.g
	 * getAllModelZips("N:\Automation\Toyota\Rawdata\Q2\EN\LEXUS\2009\RM", "N:\Automation\Toyota\Rawdata\Q1\EN\LEXUS\2009\RM")
	 * 
	 */
	/*static List<String> getAllModelZips(String ... folders) throws Exception{
		//collect all zips we need get graphic names from 
		List<String> zips = new ArrayList<String>();
		for(int i = 0; i<folders.length; i++){
			System.out.println("collecting zip file names from folder "+folders[i]);
			List<String> models = FileUtil.getAllFilesWithCertainExt(folders[i], "zip");
			for(String m: models){
				//LS460 already convertd
				//if(m.contains("09LS460"))		continue;
				String zip = folders[i] + m;
				//09SC430_RM10J0U_EN_09-12-07_UB.zip ==> 09SC430
				String model = m.substring(0, m.indexOf("_"));
				boolean existing = false;
				for(String z: zips){
					String model1 = z.substring(z.lastIndexOf("\\")+1, z.indexOf("_"));
					//pl(model1+" "+model);
					if(model1.equals(model)){
						existing = true;
					}
				}
				if(!existing){
					System.out.println(zip + " zip file name collected");
					zips.add(zip);
				}
			}
 		}
		System.out.println(zips.size() + " zip file names collected");
		return zips;
	}*/
	
	/**
	 * get all graphic names from zip files in /rawdata folder, input parameters are a list of source folders, 
	 * same zip files in former folders overwrite zip files in later folders
	 * e.g
	 * getAllGraphicNames_zip("N:\Automation\Toyota\Rawdata\Q2\EN\LEXUS\2009\RM", "N:\Automation\Toyota\Rawdata\Q1\EN\LEXUS\2009\RM")
	 * 
	 */
	/*static void getAllGraphicNames_zip(String ... folders) throws Exception{
		//collect all zips we need get graphic names from 
		List<String> zips = getAllModelZips(folders);
		
		//collect all graphic names
		int totalGraphics = 0; //including duplicates
		List<String> graphics = new ArrayList<String>();
		for(String f: zips){
			List<String> gxs = Unzip.getFileNames(f, "eps");
			for(String g:gxs){
				totalGraphics++;
				String graphicName = g.substring(g.lastIndexOf("/")+1);
				if(!graphics.contains(graphicName)){
					graphics.add(graphicName);
					//System.out.println(graphicName);
				}
			}

		}
		System.out.println(totalGraphics + " graphic name processed; " + graphics.size() + " unique names collected");
		
		//insert into database
		List<String> statements = new ArrayList<String>();
		for(String g:graphics){
			String statement = "insert into LX_OE(oe_name) values('" + g.replace(".eps", "") + "')";
			statements.add(statement);
		}
		totalGraphics = SQLUtil.batchDML(new MEPSUtil("MEPSP").con, statements);
		System.out.println(totalGraphics + " graphic name inserted into ss1819.LX_OE table");
	}*/
	
	/**
	 * traverse  through all <xref links , start with xml doc with fileNameKey
	 * <xref linkend="RM000003CRT000X_08_0006"
	 */
	
	private static void traverseXRef(Map<String, Document> docs, String fileNameKey, int depth){
		if(docs.get(fileNameKey) == null) return;
		depth++;
		NodeList xrefLst = docs.get(fileNameKey).getDocumentElement().getElementsByTagName("xref");
		String linkend;
		for(int i = 0; i<xrefLst.getLength(); i++){
			linkend = ((Element)xrefLst.item(i)).getAttribute("linkend");
			if(linkend != null && linkend.length() > 0){
				System.out.println(linkend + "(" + depth + ", parent=" + fileNameKey+ ")");
				traverseXRef(docs, linkend + ".xml", depth);
			}
		}
	}
}
