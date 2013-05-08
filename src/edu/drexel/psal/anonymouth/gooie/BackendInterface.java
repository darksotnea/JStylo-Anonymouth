package edu.drexel.psal.anonymouth.gooie;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import com.jgaap.generics.Document;

import edu.drexel.psal.anonymouth.engine.DataAnalyzer;
import edu.drexel.psal.anonymouth.engine.DocumentMagician;
import edu.drexel.psal.anonymouth.utils.ConsolidationStation;
import edu.drexel.psal.anonymouth.utils.DocumentTagger;
import edu.drexel.psal.anonymouth.utils.Tagger;
import edu.drexel.psal.jstylo.generics.Logger;
import edu.drexel.psal.jstylo.generics.ProblemSet;

public class BackendInterface {
	
	private final String NAME = "( "+this.getClass().getName()+" ) - ";
	private ProgressWindow pw;
	
	protected static BackendInterface bei = new BackendInterface();
	
	public class GUIThread implements Runnable {
		GUIMain main;
		
		public GUIThread(GUIMain main) {
			
			this.main = main;
			pw = new ProgressWindow("Processing...", main);
		}
		
		public void run() {}
	}
	
	/* ========================
	 * documents tab operations
	 * ========================
	 */
	
	// -- none --
	// all operations are fast, so no backend threads are ran.
	
	
	/**
	 * documents tab >> create new problem set
	 */
	protected static void docTabCreateNewProblemSet(GUIMain main) {
		Logger.logln("( BackendInterface ) - create new problem set");
		(new Thread(bei.new DocTabNewProblemSetButtonClick(main))).start();
	}
	
	public class DocTabNewProblemSetButtonClick extends GUIThread {
		
		public DocTabNewProblemSetButtonClick(GUIMain main) {
			super(main);
		}

		public void run() {
			Logger.logln(NAME+"Backend: create new problem set thread started.");
			
			// initialize probelm set
			main.ps = new ProblemSet();
			main.ps.setTrainCorpusName(main.defaultTrainDocsTreeName);
			GUIUpdateInterface.updateProblemSet(main);
			
			Logger.logln(NAME+"Backend: create new problem set thread finished.");
		}
	}
	
	protected static void runVerboseOutputWindow(GUIMain main){
		new Thread(bei.new RunVerboseOutputWindow(main)).start();
		
	}
	
	public class RunVerboseOutputWindow extends GUIThread{
		
			public RunVerboseOutputWindow(GUIMain main) {
			super(main);
		}

			public void run() {
				new Console();
			}
		
	}
	
	

	protected static void preTargetSelectionProcessing(GUIMain main,DataAnalyzer wizard, DocumentMagician magician){
		//Logger
		(new Thread(bei.new PreTargetSelectionProcessing(main,wizard,magician))).start();
		
	}
	
	public class PreTargetSelectionProcessing extends GUIThread {
		
		private DataAnalyzer wizard;
		private DocumentMagician magician;
		//private EditorInnerTabSpawner eits;
		
		
		public PreTargetSelectionProcessing(GUIMain main,DataAnalyzer wizard, DocumentMagician magician){
			super(main);
			//System.out.println("Entered EditTabProcessButtonClicked - NOTHING ELSE SHOULD HAPPEN UNTIL NEXT MESSAGE FROM THIS CLASS.");
			this.wizard = wizard;
			this.magician = magician;

			pw.run();
			
			//selectedIndex = main.editTP.getSelectedIndex();
			//this.eits = EditorTabDriver.eitsList.get(selectedIndex);
		}
		
		public String getDocFromCurrentTab()
		{
			return main.documentPane.getText();
		}
		
		public void run()
		{
			try
			{
				main.documentPane.setEnabled(true);
				DocumentMagician.numProcessRequests++;
				String tempDoc = "";
				
				if(DriverDocumentsTab.isFirstRun == true)
				{
					ConsolidationStation.functionWords.run();
					tempDoc = getDocFromCurrentTab();
					Logger.logln(NAME+"Process button pressed for first time (initial run) in editor tab");
					
					pw.setText("Extracting and Clustering Features...");
					try
					{
						wizard.runInitial(magician,main.cfd, main.classifiers.get(0));
						pw.setText("Extracting and Clustering Features... Done");
						pw.setText("Initializing Tagger...");

						Tagger.initTagger();
						
						pw.setText("Initialize Cluster Viewer...");
						DriverClustersWindow.initializeClusterViewer(main,false);
						pw.setText("Initialize Cluster Viewer... Done");
						pw.setText("Classifying Documents...");
						magician.runWeka();
						pw.setText("Classifying Documents... Done");
					}
					catch(Exception e){
						e.printStackTrace();
						ErrorHandler.fatalError();
					}
					
					Map<String,Map<String,Double>> wekaResults = magician.getWekaResultList();
					Logger.logln(NAME+" ****** WEKA RESULTS for session '"+ThePresident.sessionName+" process number : "+DocumentMagician.numProcessRequests);
					Logger.logln(NAME+wekaResults.toString());
					makeResultsTable(wekaResults, main);
					pw.stop();
					
				}
				else //This reprocesses
				{
					Logger.logln(NAME+"Process button pressed to re-process document to modify.");
					tempDoc = getDocFromCurrentTab();
					if(tempDoc.equals("") == true)
					{
						JOptionPane.showMessageDialog(null,
								"It is not possible to process an empty document.",
								"Document processing error",
								JOptionPane.ERROR_MESSAGE,
								GUIMain.iconNO);
					}
					else
					{
						magician.setModifiedDocument(tempDoc);
						main.documentPane.setEditable(false);
						main.documentPane.setEnabled(true);
						
						pw.setText("Extracting and Clustering Features...");
						try 
						{
							wizard.reRunModified(magician);
							pw.setText("Extracting and Clustering Features... Done");
							pw.setText("Initialize Cluster Viewer...");
							DriverClustersWindow.initializeClusterViewer(main,false);
							pw.setText("Initialize Cluster Viewer... Done");
							pw.setText("Classifying Documents...");
							magician.runWeka();
							pw.setText("Classifying Documents... Done");
						} catch (Exception e) {
							e.printStackTrace();
							ErrorHandler.fatalError();
						}
						pw.setText("Setting Results...");
						Map<String,Map<String,Double>> wekaResults = magician.getWekaResultList();
						Logger.logln(NAME+" ****** WEKA RESULTS for session '"+ThePresident.sessionName+" process number : "+DocumentMagician.numProcessRequests);
						Logger.logln(NAME+wekaResults.toString());
						makeResultsTable(wekaResults, main);
						pw.setText("Setting Results... Done");
					}
				}
				int selectedIndex = 1;
				int trueIndex = selectedIndex - 1;
				Logger.logln(NAME+"Cluster Group number '"+trueIndex+"' selected: " + DriverClustersWindow.getStringRep()[selectedIndex]);
				Logger.logln(NAME+"Cluster Group chosen by Anonymouth: "+DriverClustersWindow.getStringRep()[1]);
				DataAnalyzer.selectedTargets = DriverClustersWindow.getIntRep()[trueIndex];
				Logger.logln(NAME+"INTREP: "+DriverClustersWindow.getIntRep()[trueIndex]);//added this.
				DriverDocumentsTab.wizard.setSelectedTargets();
				DriverDocumentsTab.signalTargetsSelected(main, true);

				//eits.documentPane.setText(tempDoc);	
				//cpb.setText("Waiting for Target Selection...");
				}
				catch (Exception e)
				{
					e.printStackTrace();
					// Get current size of heap in bytes
					long heapSize = Runtime.getRuntime().totalMemory();
	
					// Get maximum size of heap in bytes. The heap cannot grow beyond this size.
					// Any attempt will result in an OutOfMemoryException.
					long heapMaxSize = Runtime.getRuntime().maxMemory();
	
					// Get amount of free memory within the heap in bytes. This size will increase
					// after garbage collection and decrease as new objects are created.
					long heapFreeSize = Runtime.getRuntime().freeMemory();
					Logger.logln(NAME+"Something happend. Here are the total, max, and free heap sizes:");
					Logger.logln(NAME+"Total: "+heapSize+" Max: "+heapMaxSize+" Free: "+heapFreeSize);
				}
		}
	}
	
	
	
	protected static void postTargetSelectionProcessing(GUIMain main,DataAnalyzer wizard, DocumentMagician magician){
		//Logger
		(new Thread(bei.new PostTargetSelectionProcessing(main,wizard,magician))).start();
		
	}
	
	public class PostTargetSelectionProcessing extends GUIThread {

		private DataAnalyzer wizard;
		private DocumentMagician magician;

		public PostTargetSelectionProcessing(GUIMain main,DataAnalyzer wizard, DocumentMagician magician){
			super(main);
			//System.out.println("Entered EditTabProcessButtonClicked - NOTHING ELSE SHOULD HAPPEN UNTIL NEXT MESSAGE FROM THIS CLASS.");
			this.wizard = wizard;
			this.magician = magician;
//			pw.run();
			//selectedIndex = main.editTP.getSelectedIndex();
			//this.eits = EditorTabDriver.eitsList.get(selectedIndex);
		}

		public void run(){
			//Scanner in = new Scanner(System.in);
			//in.nextLine();
				
			pw.setText("Target Selected");
//			TableCellRenderer renderer = new PredictionRenderer(main);
//			main.resultsTable.setDefaultRenderer(Object.class, renderer);
			DriverDocumentsTab.theFeatures = wizard.getAllRelevantFeatures();
			Logger.logln(NAME+"The Features are: "+DriverDocumentsTab.theFeatures.toString());
			//main.suggestionTable.setModel(makeSuggestionListTable(EditorTabDriver.theFeatures));
			//TableColumn tCol = main.suggestionTable.getColumnModel().getColumn(0);
			//tCol.setMaxWidth(30);
			//tCol.setMinWidth(30);
			//tCol.setPreferredWidth(30);
			// make highlight bar
			//main.highlightSelectionBox.setModel(makeHighlightBarModel());
//			main.nextSentenceButton.setEnabled(false);
//			main.prevSentenceButton.setEnabled(false);
//			main.transButton.setEnabled(false);
//			main.appendSentenceButton.setEnabled(false);
			pw.setText("Tagging all documents... Done");
			
			//main.editorProgressBar.setIndeterminate(true);	
			
			main.resultsTablePane.setOpaque(true);
			DriverDocumentsTab.okayToSelectSuggestion = true;
			
			if(DriverDocumentsTab.isFirstRun)
				ConsolidationStation.toModifyTaggedDocs.get(0).makeAndTagSentences(main.documentPane.getText(), true);
			else
				ConsolidationStation.toModifyTaggedDocs.get(0).makeAndTagSentences(main.documentPane.getText(), false);

			pw.stop();
			main.anonymityDrawingPanel.updateAnonymityBar();
			main.anonymityDrawingPanel.showPointer(true);
			for (int i = 0; i < DriverDocumentsTab.taggedDoc.getTaggedSentences().size(); i++) {
				System.out.println("		" + DriverDocumentsTab.taggedDoc.getUntaggedSentences().get(i));
				DriverDocumentsTab.originals.put(DriverDocumentsTab.taggedDoc.getUntaggedSentences().get(i), DriverDocumentsTab.taggedDoc.getTaggedSentences().get(i));
			}
			DriverDocumentsTab.originalSents = DriverDocumentsTab.taggedDoc.getUntaggedSentences();
			DriverDocumentsTab.suggestionCalculator.trackEditSentence(main);
			DriverDocumentsTab.setAllDocTabUseable(true, main);
			
			main.documentPane.setText(DriverDocumentsTab.taggedDoc.getUntaggedDocument());//must re-set the document after processing (do deal 
			int[] selectedSentInfo = DriverDocumentsTab.calculateIndicesOfSelectedSentence(0);
			DriverDocumentsTab.selectedSentIndexRange[0] = selectedSentInfo[1];
			DriverDocumentsTab.selectedSentIndexRange[1] = selectedSentInfo[2];
			DriverDocumentsTab.moveHighlight(main, DriverDocumentsTab.selectedSentIndexRange, true);
			main.documentPane.getCaret().setDot(0);
			main.documentPane.setCaretPosition(0);
			DriverDocumentsTab.charsInserted = 0; // this gets updated when the document is loaded.
			DriverDocumentsTab.charsRemoved = 0;	
			DriverDocumentsTab.caretPositionPriorToCharInsert = 0;
			Translator.firstRun = true;
			GUIMain.GUITranslator.load(DriverDocumentsTab.taggedDoc.getTaggedSentences());
			DriverDocumentsTab.isFirstRun = false;	
			
			boolean loadIfExists = false;
			
			DictionaryBinding.init();//initializes the dictionary for wordNEt
			
			DocumentTagger docTagger = new DocumentTagger();
			ArrayList<List<Document>> allDocs = magician.getDocumentSets();
			try{
				/*
				 * NOTE: This next line locks up the rest of the method until it's done. Do NOT put anything that needs to be updated
				 * Immediately after control returns to the GUI from processing after this, it will not be run until every every process here
				 * is done (It takes a long time)
				 */
				ConsolidationStation.otherSampleTaggedDocs = docTagger.tagDocs(allDocs.get(0),loadIfExists);
				ConsolidationStation.authorSampleTaggedDocs = docTagger.tagDocs(allDocs.get(1),loadIfExists);
				ConsolidationStation.setAllDocsTagged(true);
			}
			catch(Exception e){
				Logger.logln(NAME+"Oops something bad happened with the tagging of documents...");
				e.printStackTrace();
			}
			
			Logger.logln(NAME+"Finished in BackendInterface - postTargetSelection");
			//main.editorProgressBar.setIndeterminate(false);	
			main.documentPane.setEnabled(true);
            main.documentPane.setEditable(true);
//			main.nextSentenceButton.doClick();
			main.documentScrollPane.getViewport().setViewPosition(new java.awt.Point(0, 0));
			
			//cpb.setText("User Editing... Waiting to\"Re-process\"");
			
			//Logger.logln(NAME+"Writing TaggedDocument...");
			//ObjectIO.writeObject(ConsolidationStation.toModifyTaggedDocs.get(0), "toModifyDoc", ThePresident.SER_DIR);
			//Logger.logln(NAME+"TaggedDocument written...");
		}
	}
	

	
	
	public static TableModel makeSuggestionListTable(String[] suggestions){
		int numSuggestions = suggestions.length;
		String[] skip = {"COMPLEXITY","FLESCH_READING_EASE_SCORE","GUNNING_FOG_READABILITY_INDEX","AVERAGE_SENTENCE_LENGTH"};
		int i=0;
		int numDesiredSuggestions = numSuggestions - skip.length;
		DriverDocumentsTab.suggestionToAttributeMap = new HashMap<Integer,Integer>(numDesiredSuggestions);
		String[][] theModel = new String[numDesiredSuggestions][2]; 
		int j=0;
		i = 0;
		int k = 0;
		boolean shouldSkip = false;
		while(i<numDesiredSuggestions){
			//System.out.println("SUGGESTION: "+suggestions[j]);
			shouldSkip =false;
			for(k=0;k<skip.length;k++){
				//System.out.println(">"+suggestions[i]+"<>"+skip[k]+"<");
				if(skip[k].equals(suggestions[j])){
					shouldSkip = true;
					break;
				}
			}
			if(shouldSkip == true){
				//System.out.println("won't add "+suggestions[j]+" to suggestion list.");
				j++;
				continue;
			}
			theModel[i][0] = Integer.toString((i+1));
			theModel[i][1] = suggestions[j];
			DriverDocumentsTab.suggestionToAttributeMap.put(i,j);
			j++;
			i++;
		}
		TableModel suggestionModel = new DefaultTableModel(theModel,new String[]{"No.","Feature Name"});
		return suggestionModel;
	}
	
	
	public static void makeResultsTable(Map<String,Map<String,Double>> resultMap, GUIMain main)
	{
		main.resultsTableModel.getDataVector().removeAllElements();
		
		Iterator<String> mapKeyIter = resultMap.keySet().iterator();
		Map<String,Double> tempMap = resultMap.get(mapKeyIter.next()); 
		
		int numAuthors = DocumentMagician.numSampleAuthors+1;
		
		Object[] authors = (tempMap.keySet()).toArray();
		Double[] predictions = new Double[authors.length];
		Map<Double, Object> predMap = new HashMap<Double, Object>();
		
		Object[] keyRing = tempMap.values().toArray();
		int maxIndex = 0;
		Double biggest = .01;
		for(int i = 0; i < numAuthors; i++){
			Double tempVal = ((Double)keyRing[i])*100;
			// compare PRIOR to rounding.
			if(biggest < tempVal){
				biggest = tempVal;
				maxIndex = i;
			}
			int precision = 100;
			tempVal = Math.floor(tempVal*precision+.5)/precision;	
			predictions[i] = tempVal;
			
			if (authors[i].equals("~* you *~")) {
				System.out.println("PASSED!!!");
				predMap.put(predictions[i], "You");
			} else
				predMap.put(predictions[i], authors[i]);
		}
		
		Arrays.sort(predictions);
		
		for (int i = numAuthors-1; i >= 0; i--)
		{
			main.resultsTableModel.addRow(new Object[]{predMap.get(predictions[i]), predictions[i] + "%"});
		}
		
		DriverDocumentsTab.resultsMaxIndex = maxIndex;
		DriverDocumentsTab.chosenAuthor = (String)authors[maxIndex];
		DriverDocumentsTab.maxValue = (Object)biggest;
	}
	
}

class PredictionRenderer implements TableCellRenderer {
	
	private GUIMain main;
	
	public static final DefaultTableCellRenderer DEFAULT_RENDERER = new DefaultTableCellRenderer();
	
	public PredictionRenderer(GUIMain main)
	{
		this.main = main;
		this.main.chosenAuthor = DriverDocumentsTab.chosenAuthor;
		this.main.resultsMaxIndex = DriverDocumentsTab.resultsMaxIndex;
	}
	  
	  
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) 
	{
		Component renderer = DEFAULT_RENDERER.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	    ((JLabel) renderer).setOpaque(true);
	    Color foreground, background;
	    
	      if ((column  == main.resultsMaxIndex) && (row==0)) {
		    	 if(main.chosenAuthor.equals(DocumentMagician.authorToRemove)){
		        foreground = Color.black;
		        background = Color.red;
		      } else {
		        foreground = Color.black;
		        background = Color.green;
		      }
	      }
	      else{
	    	  	foreground = Color.black;
	    	  	background = Color.white;
	      }
	    
	    renderer.setForeground(foreground);
	    renderer.setBackground(background);
	    return renderer;
	}
}
