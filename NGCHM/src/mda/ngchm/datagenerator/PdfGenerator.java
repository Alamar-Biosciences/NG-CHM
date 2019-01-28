/*******************************************************************
 * CLASS: PdfGenerator
 *
 * This class instantiates an PdfGenerator object for use in generating
 * a PDF version of the heat map being generated by the 
 * HeatmapDataGenerator process. 
 * 
 * Author: Mark Stucky
 * Date: March 8, 2016
 ******************************************************************/

package mda.ngchm.datagenerator;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;

import static mda.ngchm.datagenerator.ImportConstants.*;

public class PdfGenerator { 
	private int rowClassAdjustment = 0;

	/*******************************************************************
	 * METHOD: createHeatmapPDF
	 *
	 * This method is the main driver for generating a heat map PDF 
	 * document using the PdfBox library.  A page is created for each 
	 * data layer and a legend page is created at the end of the PDF.
	 ******************************************************************/
	public void createHeatmapPDF(ImportData iData, boolean fullPDF) {
		PDDocument doc = null;
		BufferedImage image;
		try {
		    doc = new PDDocument();
			for (int i=0; i < iData.matrixImages.size(); i++) {
				image = iData.matrixImages.get(i); 
				if (fullPDF) {
					createFullPDFHeatmapPage(doc,image,iData);
				} else {
					createPDFHeatmapPage(doc, image, iData);
				}
			}
			if ((iData.rowData.getVisibleClasses().size() > 0) || (iData.colData.getVisibleClasses().size() > 0)) {
				createPDFLegendPage(doc, iData);
			}
		    doc.save(iData.outputDir+File.separator+iData.chmName+".pdf");
		    doc.close();
		} catch (Exception ex) {
			System.out.println("Exception in PdfGenerator.createHeatmapPDF: " + ex.toString());
	        ex.printStackTrace();
		} finally {
		    if( doc != null ) {
		    	try {
			        doc.close();
		    	} catch (Exception ex) {
		    		//do nothing}
		    	}
		    }
		}
	}	
	
	public PDPageContentStream getPdfPage(PDDocument doc, ImportData iData) throws Exception {
        PDPage page = new PDPage();
        doc.addPage(page);
        PDPageContentStream contentStream = new PDPageContentStream(doc, page);
        //Write header and footer to PDF Document
        drawHeaderFooter(doc, contentStream, iData.chmName, US_LETTER_WIDTH, US_LETTER_HEIGHT);
		return contentStream;
	}
	
	public PDPageContentStream getPdfPageCustomSize(PDDocument doc, ImportData iData, int mapWidth, int mapHeight) throws Exception {
		int pageWidth = PDF_DENDRO_HEIGHT + mapWidth + 300;
		int pageHeight = PDF_CONTENT_START + PDF_DENDRO_HEIGHT + mapHeight + 300;
		PDRectangle pageDim = new PDRectangle(pageWidth, pageHeight); 
        PDPage page = new PDPage(pageDim);
        doc.addPage(page);
        PDPageContentStream contentStream = new PDPageContentStream(doc, page);
        //Write header and footer to PDF Document
        drawHeaderFooter(doc, contentStream, iData.chmName,pageWidth, pageHeight);
		return contentStream;
	}
   
	public void createPDFHeatmapPage(PDDocument doc, BufferedImage image, ImportData iData) {
		try {
            PDPageContentStream contentStream = getPdfPage(doc, iData);
            int[] rowColPos = getStartingPositions(iData,US_LETTER_HEIGHT);
            //Draw row dendrogram on PDF
            if (iData.colData.dendroMatrix != null) {
	            rowColPos = drawColumnDendrogram(doc, contentStream, iData, rowColPos, PDF_MAP_SIZE);
            }
            //Draw column covariates on PDF
            rowColPos = drawColumnCovariates(doc, contentStream, iData, rowColPos, PDF_MAP_SIZE);
            //Draw row dendrogram on PDF
            rowColPos[PDF_ROW_POS] -= PDF_MAP_SIZE;
            if (iData.rowData.dendroMatrix != null) {
                rowColPos = drawRowDendrogram(doc, contentStream, iData, rowColPos, PDF_MAP_SIZE);
            }
            //Draw row covariates on PDF
            rowColPos = drawRowCovariates(doc, contentStream, iData, rowColPos, PDF_MAP_SIZE);
            //Draw heat map on PDF
            PDImageXObject  pdImageXObject = LosslessFactory.createFromImage(doc, image);
            contentStream.drawImage(pdImageXObject, rowColPos[PDF_COL_POS], rowColPos[PDF_ROW_POS], PDF_MAP_SIZE, PDF_MAP_SIZE);
            if (iData.rowData.topItems != null) {
                rowColPos = drawRowTopItems(doc, contentStream, iData, rowColPos);
            }
            if (iData.colData.topItems != null) {
            	rowColPos = drawColTopItems(doc, contentStream, iData, rowColPos);
            }
            contentStream.close();
		} catch (Exception ex) {
			System.out.println("Exception in PdfGenerator.createPDFHeatmapPage: " + ex.toString());
	        ex.printStackTrace();
		} 
	}	
	

	public void createFullPDFHeatmapPage(PDDocument doc, BufferedImage image, ImportData iData) {
		try {
			int mapWidth = iData.colData.classArray.length*5-5;
			int mapHeight = iData.rowData.classArray.length*5-5;
            PDPageContentStream contentStream = getPdfPageCustomSize(doc, iData, mapWidth, mapHeight);
            int[] rowColPos = getStartingPositions(iData,PDF_CONTENT_START + PDF_DENDRO_HEIGHT + mapHeight + 300);
            //Draw row dendrogram on PDF
            if (iData.colData.dendroMatrix != null) {
	            rowColPos = drawColumnDendrogram(doc, contentStream, iData, rowColPos, mapWidth);
            }
            //Draw column covariates on PDF
            rowColPos = drawColumnCovariates(doc, contentStream, iData, rowColPos, mapWidth);
            //Draw row dendrogram on PDF
            rowColPos[PDF_ROW_POS] -= mapHeight;
            if (iData.rowData.dendroMatrix != null) {
                rowColPos = drawRowDendrogram(doc, contentStream, iData, rowColPos, mapHeight);
            }
            //Draw row covariates on PDF
            rowColPos = drawRowCovariates(doc, contentStream, iData, rowColPos, mapHeight);
            //Draw all the col labels
            rowColPos = drawAllColLabels(doc, contentStream, iData, rowColPos);
            //Draw all the row labels
            rowColPos = drawAllRowLabels(doc, contentStream, iData, rowColPos, mapWidth, mapHeight);
            //Draw heat map on PDF
            PDImageXObject  pdImageXObject = LosslessFactory.createFromImage(doc, image);
            contentStream.drawImage(pdImageXObject, rowColPos[PDF_COL_POS], rowColPos[PDF_ROW_POS], mapWidth, mapHeight);
            createPDFDataDistributionPlot(doc, iData);
            contentStream.close();
		} catch (Exception ex) {
			System.out.println("Exception in PdfGenerator.createPDFHeatmapPage: " + ex.toString());
	        ex.printStackTrace();
		} 
	}	
	
	public int[] getStartingPositions(ImportData iData, int pageHeight) {
        int[] rowColPos = new int[2];
        try {
	        rowColPos[PDF_ROW_POS] = pageHeight - PDF_CONTENT_START;
	    	List<InputClass> icList = iData.rowData.getVisibleClasses();
	    	int classAdj = 0;
	        for (int i = 0; i <  icList.size(); i++) {
	        	InputClass ic = icList.get(i);
	        	if (ic.barType.equals(COLOR_PLOT)) {
	        		classAdj += PDF_CLASS_HEIGHT;
	        	} else {
	        		classAdj += PDF_CLASS_HEIGHT*3;
	        	}
	        }
	        rowClassAdjustment = classAdj;
	        rowColPos[PDF_COL_POS] = 10 + classAdj+ 2;
	        if (iData.rowData.dendroMatrix != null) {
	        	 rowColPos[PDF_COL_POS] += PDF_DENDRO_HEIGHT;
	        }
		} catch (Exception ex) {
			System.out.println("Exception in PdfGenerator.getStartingPositions: " + ex.toString());
	        ex.printStackTrace();
		} 
        return rowColPos;
	}

	public void createPDFDataDistributionPlot(PDDocument doc, ImportData iData) {
		try {
	           PDPageContentStream contentStream = getPdfPage(doc, iData);
	           int missingCount = iData.matrixFiles.get(0).missingCount;
	           int[] countBins = iData.matrixFiles.get(0).distributionCounts;
	           float[] thresh = iData.matrixFiles.get(0).distributionBreaks;
	           int binNums = countBins.length;
	           int threshNums = thresh.length;
	           int maxCount = iData.matrixFiles.get(0).missingCount;
	           for (int a = 0; a < binNums; a++) {
	        	   if (countBins[a] > maxCount) {
	        		   maxCount = countBins[a];
	        	   }
	           }
	           int histStartY = 712;
	           writePDFText(contentStream, "Data Distribution", 12, PDF_FONT_BOLD, 10, histStartY, false);
	           histStartY -= 15;
	           PDImageXObject  pdMatrixDistributionLegend = LosslessFactory.createFromImage(doc, iData.matrixFiles.get(0).distributionLegend);
	           contentStream.drawImage(pdMatrixDistributionLegend, 50, histStartY-110, 110, 111);
	           BufferedImage breakTick = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
	           breakTick.setRGB(0, 0, RGB_BLACK);
	           breakTick.setRGB(1,0,RGB_BLACK);
	           PDImageXObject pdImageBreakTick = LosslessFactory.createFromImage(doc, breakTick);
	           contentStream.drawImage(pdImageBreakTick, 48, histStartY, 2, 1);
	           contentStream.drawImage(pdImageBreakTick, 48, histStartY-100, 2, 1);
	           contentStream.drawImage(pdImageBreakTick, 48, histStartY-110, 2, 1);
	           for (int i = 0; i < threshNums; i++) {
	        	   float roundThreshK = Math.round(thresh[i]*1000);
	        	   float roundThresh = roundThreshK/1000;
	        	   PDFont font = PDF_FONT;
	        	   float textWidth = font.getStringWidth(Float.toString(roundThresh))/1000.0f * 12;
	        	   writePDFText(contentStream, Float.toString(roundThresh), 12, PDF_FONT, 45-(int) textWidth, histStartY-(i+1)*10-5, false);
	        	   writePDFText(contentStream, "n = " + Integer.toString(countBins[i]), 12, PDF_FONT, 55+(110*countBins[i]/maxCount) + 2, histStartY-(i+1)*10+1, false);
	        	   contentStream.drawImage(pdImageBreakTick, 48, histStartY-(i+1)*10, 2, 1);
	           }
	           
	           writePDFText(contentStream, "n = " + Integer.toString(countBins[binNums-1]), 12, PDF_FONT, 55+(110*countBins[binNums-1]/maxCount) + 2, histStartY-(threshNums+1)*10+1, false);
	           writePDFText(contentStream, "Missing", 12, PDF_FONT, 5, histStartY-120+10, false);
	           writePDFText(contentStream, "n = " + Integer.toString(missingCount), 12, PDF_FONT, 55+(110*missingCount/maxCount) + 2, histStartY-(binNums+1)*10+1, false);

	           contentStream.close();
			} catch (Exception ex) {
				System.out.println("Exception in PdfGenerator.createPDFLegendPage: " + ex.toString());
		        ex.printStackTrace();
			}
	}
	
	public void createPDFLegendPage(PDDocument doc, ImportData iData) {
		try {
           PDPageContentStream contentStream = getPdfPage(doc, iData);
           List<InputClass> rcFiles = iData.rowData.getVisibleClasses(); 
           List<InputClass> ccFiles = iData.colData.getVisibleClasses(); 
           BufferedImage borderBox = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
           borderBox.setRGB(0, 0, RGB_BLACK);
           PDImageXObject pdImageBorderBox = LosslessFactory.createFromImage(doc, borderBox);
           int rowCovStartY = 712;
   		   if (rcFiles.size() > 0)  {
   			  writePDFText(contentStream, "Row Covariate Bar Legends", 12, PDF_FONT_BOLD, 10, rowCovStartY, false);
   			  rowCovStartY -= 15;
   			  float pairs = new Float(rcFiles.size())/2;
   			  int next = 0;
   			  //Get number of pairs of row class legends
   			  for (int i=0; i<pairs; i++) {
   				int legend2 = -1;
   				if ((pairs % 1) == 0) {
   					legend2 = next+1;
   				}
   				//IF legend pair fits on page, write to page ELSE create new page and write to it.
   				if (legendFitsOnPage(doc, rcFiles, contentStream, next, legend2, rowCovStartY)) {
   					rowCovStartY = writeLegend(doc, rcFiles, contentStream, next, legend2, rowCovStartY, pdImageBorderBox);
   				} else {
   					rowCovStartY = 712;
   					contentStream.close();
   					contentStream = getPdfPage(doc, iData);
   	   			    writePDFText(contentStream, "Row Covariate Bar Legends (continued)", 12, PDF_FONT_BOLD, 10, rowCovStartY, false);
   	   			    rowCovStartY -= 15;
   					rowCovStartY = writeLegend(doc, rcFiles, contentStream, next, legend2, rowCovStartY, pdImageBorderBox);
  				}
   				next += 2;
   			  }
   		   }
   		   if (ccFiles.size() > 0)  {
   			   writePDFText(contentStream, "Column Covariate Bar Legends", 12, PDF_FONT_BOLD, 10, rowCovStartY, false);
   	           rowCovStartY -= 15;
 			   float pairs = new Float(ccFiles.size())/2;
			   int next = 0;
   			   for (int i=0; i<pairs; i++) {
   				   int legend2 = -1;
   				   if ((pairs % 1) == 0) {
   					   legend2 = next+1;
   				   }
       			   if (legendFitsOnPage(doc, ccFiles, contentStream, next, legend2, rowCovStartY)) {
      					rowCovStartY = writeLegend(doc, ccFiles, contentStream, next, legend2, rowCovStartY, pdImageBorderBox);
       				} else {
       					rowCovStartY = 712;
       					contentStream.close();
       					contentStream = getPdfPage(doc, iData);
       	   			    writePDFText(contentStream, "Column Covariate Bar Legends (continued)", 12, PDF_FONT_BOLD, 10, rowCovStartY, false);
       	   			    rowCovStartY -= 15;
       					rowCovStartY = writeLegend(doc, ccFiles, contentStream, next, legend2, rowCovStartY, pdImageBorderBox);
      				}
   				   next += 2;
   			   }
   		   }
           contentStream.close();
		} catch (Exception ex) {
			System.out.println("Exception in PdfGenerator.createPDFLegendPage: " + ex.toString());
	        ex.printStackTrace();
		} 
	}	
   
	public int writeLegend(PDDocument doc, List<InputClass> classFiles, PDPageContentStream contentStream, int legend1, int legend2, int rowCovStartY, PDImageXObject pdImageBorderBox) {
        int legend1Rows = 0;
		try {
			int currRow = rowCovStartY;
			InputClass iFile = classFiles.get(legend1);
			String[]rowValues;
			int colStart = 0;
	        rowValues = iFile.orderedClass;
			int breakSize = iFile.map.breaks.size()+1;
	      	if (iFile.map.type.equals("continuous")) {
	      		breakSize = iFile.map.contBreaks.size()+1;
	      	}
			writePDFText(contentStream, iFile.name, 10, PDF_FONT_BOLD, colStart+14, currRow, false);
			currRow -= (breakSize*10)+5;
			//Write legend class totals to the PDF
			if (iFile.map.type.equals("continuous")) {
	  	        writeContinuousCovariateClassTotals(contentStream, iFile, rowValues, currRow, colStart);
			} else {
	  	        writeDiscreteClassTotals(contentStream, iFile, rowValues, currRow, colStart);
			}
	        //Draw covariate legend on the PDF 
			PDImageXObject  pdImageClassXObjectC = null;
			if (iFile.barType.equals(COLOR_PLOT)) {
		        pdImageClassXObjectC = LosslessFactory.createFromImage(doc, iFile.classLegend);
		        contentStream.drawImage(pdImageBorderBox, colStart+17, currRow-1, 12, (breakSize*10)+2);
		        contentStream.drawImage(pdImageClassXObjectC, colStart+18, currRow, 10, breakSize*10);
			}
	        legend1Rows = currRow;
	        currRow = rowCovStartY;
	        if (legend2 > 0) {
	        	colStart = 300;
	        	iFile = (InputClass) classFiles.get(legend2);
		        rowValues = iFile.orderedClass;
				breakSize = iFile.map.breaks.size()+1;
		      	if (iFile.map.type.equals("continuous")) {
		      		breakSize = iFile.map.contBreaks.size()+1;
		      	}
				writePDFText(contentStream, iFile.name, 10, PDF_FONT_BOLD, colStart+14, currRow, false);
				currRow -= (breakSize*10)+5;
				//Write legend class totals to the PDF
				if (iFile.map.type.equals("continuous")) {
		  	        writeContinuousCovariateClassTotals(contentStream, iFile, rowValues, currRow, colStart);
				} else {
					writeDiscreteClassTotals(contentStream, iFile, rowValues, currRow, colStart);
				}
		        //Draw covariate legend on the PDF 
				if (iFile.barType.equals(COLOR_PLOT)) {
			        pdImageClassXObjectC = LosslessFactory.createFromImage(doc, iFile.classLegend);
			        contentStream.drawImage(pdImageBorderBox, colStart+16, currRow-1, 12, (breakSize*10)+2);
			        contentStream.drawImage(pdImageClassXObjectC, colStart+17, currRow, 10, breakSize*10);
				}
		        if (currRow < legend1Rows) {
		        	legend1Rows = currRow;
		        }
	        }
	        legend1Rows -= 45;
		} catch (Exception ex) {
			System.out.println("Exception in PdfGenerator.writeLegend: " + ex.toString());
	        ex.printStackTrace();
		} 
		return legend1Rows;
	}
	
	public boolean legendFitsOnPage(PDDocument doc, List<InputClass> classFiles, PDPageContentStream contentStream, int legend1, int legend2, int rowCovStartY) {
		boolean itFits = true;
		try {
			int currRow = rowCovStartY;
			InputClass iFile = (InputClass) classFiles.get(legend1);
			int breakSize = iFile.map.breaks.size()+1;
	      	if (iFile.map.type.equals("continuous")) {
	      		breakSize = iFile.map.contBreaks.size()+1;
	      	}
			currRow -= (breakSize*10)+5;
	        int legend1Rows = currRow;
	        currRow = rowCovStartY;
	        if (legend2 > 0) {
	        	iFile = (InputClass) classFiles.get(legend2);
				breakSize = iFile.map.breaks.size();
		      	if (iFile.map.type.equals("continuous")) {
		      		breakSize = iFile.map.contBreaks.size();
		      	}
				currRow -= (breakSize*10)+5;
		        if (currRow < legend1Rows) {
		        	legend1Rows = currRow;
		        }
	        }
	        if (legend1Rows < 50) {
	        	itFits = false;
	        }
		} catch (Exception ex) {
			System.out.println("Exception in PdfGenerator.legendFitsOnPage: " + ex.toString());
	        ex.printStackTrace();
		} 
		return itFits;
	}	
		
   public void writeDiscreteClassTotals(PDPageContentStream contentStream, InputClass iFile, String[] covValues, int rowCovStartY, int colStart) {
	   try {
	   		int[] covTotals = new int[iFile.map.breaks.size()+1];
	        for (int j = 1; j < covValues.length; j++) {
	        	String elemValue = covValues[j];
	        	if ((elemValue == null) || (NA_VALUES.contains(elemValue)) ){
	        		covTotals[covTotals.length-1]++;
	        	} else if (elemValue.equals(CUT_VALUE)) {
	        		// Do nothing (no need to count the cut row/col)
	        	} else {
		        	for (int k = 0; k < iFile.map.breaks.size(); k++) {
		        		if (elemValue.equals(iFile.map.breaks.get(k))) {
		        			covTotals[k]++;
		        		}
		        	}
	        	}
	        } 
	        int revOrder = covTotals.length - 1;
	        for (int k = 0; k < covTotals.length;k++) {
	        	String breakVal;
	        	if (k == covTotals.length - 1) {
	        		breakVal = "Missing Value";
	        	} else {
	            	breakVal = iFile.map.breaks.get(k);
	        	}
	        	if (breakVal.length() > 25) {
	        		breakVal = breakVal.substring(0,25)+"...";
	        	}
			    writePDFText(contentStream, breakVal, 7, PDF_FONT, colStart+35, (rowCovStartY+2)+(10*revOrder), false);
			    writePDFText(contentStream, "n = "+ covTotals[k], 7, PDF_FONT, colStart+150, (rowCovStartY+2)+(10*revOrder), false);
			    revOrder--;
	        }
		} catch (Exception ex) {
			System.out.println("Exception in PdfGenerator.writeDiscreteClassTotals: " + ex.toString());
	        ex.printStackTrace();
		} 
   }
   
   public void writeContinuousCovariateClassTotals(PDPageContentStream contentStream, InputClass iFile, String[] covValues, int rowCovStartY, int colStart) throws Exception {
	   try {
		   int[] covTotals = new int[iFile.map.contBreaks.size()+1];
	       for (int j = 1; j < covValues.length; j++) {
		       	String elemValue = covValues[j];
	        	if ((elemValue == null) || (NA_VALUES.contains(elemValue)) ){
		       		covTotals[iFile.map.contBreaks.size()]++;
		       	} else if (elemValue.equals(CUT_VALUE)) {
		       		//Do nothing (no need to count the cut row/col)
		       	} else {
			       	for (int k = 0; k < iFile.map.contBreaks.size(); k++) {
						if (k == 0 && Float.valueOf(elemValue) < Float.valueOf(iFile.map.contBreaks.get(k))){
							covTotals[k]++;
						} else if (k == iFile.map.contBreaks.size() - 1 && Float.valueOf(elemValue) > Float.valueOf(iFile.map.contBreaks.get(k))){
							covTotals[k]++;
						} else if (Float.valueOf(elemValue) <= Float.valueOf(iFile.map.contBreaks.get(k))){
							covTotals[k]++;
							break;
						}
			       	}
		    	}
	       } 
	       int revOrder = covTotals.length - 1;
	       for (int k = 0; k < covTotals.length;k++) {
	        	String breakVal;
	        	if (k == covTotals.length - 1) {
	        		breakVal = "Missing Value";
	        	} else {
	            	breakVal = iFile.map.contBreaks.get(k);
	        	}
	        	if (breakVal.length() > 25) {
	        		breakVal = breakVal.substring(0,25)+"...";
	        	}
			    writePDFText(contentStream, breakVal, 7, PDF_FONT, colStart+35, (rowCovStartY+2)+(10*revOrder), false);
			    writePDFText(contentStream, "n = "+ covTotals[k], 7, PDF_FONT, colStart+150, (rowCovStartY+2)+(10*revOrder), false);
			    revOrder--;
	       }
		} catch (Exception ex) {
			System.out.println("Exception in PdfGenerator.writeContinuousCovariateClassTotals: " + ex.toString());
	        ex.printStackTrace();
		} 
  }
   
	public void drawHeaderFooter(PDDocument doc, PDPageContentStream contentStream, String mapName, int pageWidth, int pageHeight) {
        try {
        	//Write heatmap name to header (truncating if necessary)
        	PDFont font = PDF_FONT_BOLD;
        	int fontSize = 14;
        	float strWidth = font.getStringWidth(mapName)/1000 * fontSize;
        	float strSpace = pageWidth - 130-10;
        	while (strWidth > strSpace) { // trim down 5 letters at a time to fit space allocated for map name
	        	mapName = mapName.substring(0,mapName.length()-5) + "...";
	        	strWidth = font.getStringWidth(mapName)/1000 * fontSize;
	        }
			writePDFText(contentStream, mapName, fontSize, PDF_FONT_BOLD, 130, pageHeight - 37, false);
			//Draw MDA logo on header
			InputStream is = getClass().getResourceAsStream("/images/mdabcblogo262x108.png");
	        BufferedImage mdaLogoImg = ImageIO.read(is);
	        PDImageXObject  mdaLogo = LosslessFactory.createFromImage(doc, mdaLogoImg);
	        contentStream.drawImage(mdaLogo, 10, pageHeight - 52, 100, 40);
			//Draw red bar on header
	        BufferedImage redBarImg = ImageIO.read(getClass().getResourceAsStream("/images/redbar.png"));
	        PDImageXObject  redBar = LosslessFactory.createFromImage(doc, redBarImg);
	        contentStream.drawImage(redBar, 10, pageHeight - 57, pageWidth - 22, 12);
			//Draw In Silico logo on footer
	        BufferedImage insilicoLogoImg = ImageIO.read(getClass().getResourceAsStream("/images/insilicologo.png"));
	        PDImageXObject  insilicoLogo = LosslessFactory.createFromImage(doc, insilicoLogoImg);
	        contentStream.drawImage(insilicoLogo, 10, 10, 70, 22);
		} catch (Exception ex) {
			System.out.println("Exception in PdfGenerator.drawHeaderFooter: " + ex.toString());
	        ex.printStackTrace();
		} 
	}

	public int[] drawColumnDendrogram(PDDocument doc, PDPageContentStream contentStream, ImportData iData, int[] posArray, int mapWidth) {
		posArray[PDF_ROW_POS] -= PDF_DENDRO_HEIGHT;
        try {
            PDImageXObject  pdColDendroImageXObject = LosslessFactory.createFromImage(doc, iData.colData.dendroImage);
            contentStream.drawImage(pdColDendroImageXObject, posArray[PDF_COL_POS], posArray[PDF_ROW_POS], mapWidth, PDF_DENDRO_HEIGHT);
		} catch (Exception ex) {
			System.out.println("Exception in PdfGenerator.drawColumnDendrogram: " + ex.toString());
	        ex.printStackTrace();
		} 
        return posArray;
	}
	
	public int[] drawColumnCovariates(PDDocument doc, PDPageContentStream contentStream, ImportData iData, int[] posArray, int mapWidth) {
        try {
        	List<InputClass> icList = iData.colData.getVisibleClasses();
        	posArray[PDF_ROW_POS] -= 2;
        	boolean containsBar = false;
            for (int i = 0; i <  icList.size(); i++) {
            	InputClass icl = (InputClass) icList.get(i);
	            if (!icl.barType.equals(COLOR_PLOT)) {
	            	containsBar = true;
	            }
            }
            for (int i = 0; i <  icList.size(); i++) {
            	InputClass ic = (InputClass) icList.get(i);
	            PDImageXObject  pdImageClassXObjectC = LosslessFactory.createFromImage(doc, ic.classImage);
	            int classHeight = PDF_CLASS_HEIGHT;
	            int horizPos = posArray[PDF_COL_POS]+mapWidth+2;
	            int midPos = posArray[PDF_ROW_POS]-3;
	            if (!ic.barType.equals(COLOR_PLOT)) {
	            	classHeight = PDF_CLASS_HEIGHT*3;
	            	int topPos = posArray[PDF_ROW_POS]-i;
	            	int botPos = posArray[PDF_ROW_POS]-classHeight+2-i;
	            	midPos = botPos+((topPos-botPos)/2);
	            	writePDFText(contentStream, "-", 5, PDF_FONT_BOLD, horizPos, topPos, false);
		    		writePDFText(contentStream, "-", 5, PDF_FONT_BOLD, horizPos, midPos, false);
		    		writePDFText(contentStream, "-", 5, PDF_FONT_BOLD, horizPos, botPos, false);
		    		horizPos += 3;
	        		String midStr = String.valueOf(Math.round(Float.parseFloat(ic.lowBound)+((Float.parseFloat(ic.highBound)-Float.parseFloat(ic.lowBound))/2)));
	            	String lowStr = Float.parseFloat(ic.highBound) >= 2 ? ic.lowBound.replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1") : ic.lowBound;
	            	String highStr = Float.parseFloat(ic.highBound) >= 2 ? ic.highBound.replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1") : ic.highBound;
	            	midStr = Float.parseFloat(ic.highBound) >= 2 ? midStr.replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1") : midStr;
		    		writePDFText(contentStream, lowStr, 4, PDF_FONT_BOLD, horizPos, topPos-2, false);
		    		writePDFText(contentStream, midStr, 4, PDF_FONT_BOLD, horizPos, midPos, false);
		    		writePDFText(contentStream, highStr, 4, PDF_FONT_BOLD, horizPos, botPos+2, false);
	            	horizPos +=8;
	            } else {
	            	if (containsBar) horizPos += 11;
	            }
	            posArray[PDF_ROW_POS] -= classHeight-2;
	            contentStream.drawImage(pdImageClassXObjectC, posArray[PDF_COL_POS], posArray[PDF_ROW_POS], mapWidth, classHeight - 1);
            	String covName = ic.name;
    			covName = covName.length() > 20 ? covName.substring(0, 20)+"..." : covName;
	    		writePDFText(contentStream, covName, 5, PDF_FONT_BOLD, horizPos, midPos, false);
            }
       	    posArray[PDF_ROW_POS] -= 2;
		} catch (Exception ex) {
			System.out.println("Exception in PdfGenerator.drawColumnCovariates: " + ex.toString());
	        ex.printStackTrace();
		} 
        return posArray;
	}
	
	public int[] drawRowDendrogram(PDDocument doc, PDPageContentStream contentStream, ImportData iData, int[] posArray, int mapHeight) {
        try {
        	PDImageXObject  pdRowDendroImageXObject = LosslessFactory.createFromImage(doc, iData.rowData.dendroImage);
            contentStream.drawImage(pdRowDendroImageXObject, 10, posArray[PDF_ROW_POS], PDF_DENDRO_HEIGHT, mapHeight);
		} catch (Exception ex) {
			System.out.println("Exception in PdfGenerator.drawRowDendrogram: " + ex.toString());
	        ex.printStackTrace();
		} 
        return posArray;
	}
	
	public int[] drawRowCovariates(PDDocument doc, PDPageContentStream contentStream, ImportData iData, int[] posArray, int mapHeight) throws Exception {
    	List<InputClass> icList = iData.rowData.getVisibleClasses();
        int colStartPos = posArray[PDF_COL_POS] - (rowClassAdjustment+1);
        try {
        	boolean containsBar = false;
            for (int i = 0; i <  icList.size(); i++) {
            	InputClass icl = (InputClass) icList.get(i);
	            if (!icl.barType.equals(COLOR_PLOT)) {
	            	containsBar = true;
	            }
            }
            for (int i = 0; i <  icList.size(); i++) {
        		int vertPos = posArray[PDF_ROW_POS]-2;
            	InputClass ic = (InputClass) icList.get(i);
	            int classHeight = PDF_CLASS_HEIGHT;
	            int classLabelAdj = 2;
	        	int midPos = colStartPos+classLabelAdj;
	            if (!ic.barType.equals(COLOR_PLOT)) {
	            	classHeight = PDF_CLASS_HEIGHT*3;
	            	classLabelAdj += classHeight/2;
	            	int leftPos = colStartPos-1;
	            	int rightPos = (leftPos+classHeight)-1;
	            	midPos = leftPos+((rightPos-leftPos)/2);
	            	writePDFText(contentStream, "-", 5, PDF_FONT_BOLD, leftPos, vertPos, true);
	            	writePDFText(contentStream, "-", 5, PDF_FONT_BOLD, midPos, vertPos, true);
	            	writePDFText(contentStream, "-", 5, PDF_FONT_BOLD, rightPos, vertPos, true);
	            	vertPos -= 3;
	        		String midStr = String.valueOf(Math.round(Float.parseFloat(ic.lowBound)+((Float.parseFloat(ic.highBound)-Float.parseFloat(ic.lowBound))/2)));
	            	String lowStr = Float.parseFloat(ic.highBound) >= 2 ? ic.lowBound.replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1") : ic.lowBound;
	            	String highStr = Float.parseFloat(ic.highBound) >= 2 ? ic.highBound.replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1") : ic.highBound;
	            	midStr = Float.parseFloat(ic.highBound) >= 2 ? midStr.replaceFirst("\\.0*$|(\\.\\d*?)0+$", "$1") : midStr;
	        		writePDFText(contentStream, lowStr, 4, PDF_FONT_BOLD, leftPos+2, vertPos, true);
	        		writePDFText(contentStream, midStr, 4, PDF_FONT_BOLD, midPos, vertPos, true);
	        		writePDFText(contentStream, highStr, 4, PDF_FONT_BOLD, rightPos-2, vertPos, true);
	        		vertPos -= 8;
	            } else {
	            	if (containsBar) vertPos -= 11;
	            }
	            PDImageXObject  pdImageClassXObjectR = LosslessFactory.createFromImage(doc, ic.classImage);
	            contentStream.drawImage(pdImageClassXObjectR, colStartPos, posArray[PDF_ROW_POS], classHeight - 1, mapHeight);
            	String covName = ic.name;
    			covName = covName.length() > 20 ? covName.substring(0, 20)+"..." : covName;
	    		writePDFText(contentStream, covName, 5, PDF_FONT_BOLD, midPos, vertPos, true);
            	colStartPos += classHeight;
            }
		} catch (Exception ex) {
			System.out.println("Exception in PdfGenerator.drawRowCovariates: " + ex.toString());
	        ex.printStackTrace();
		} 
        return posArray;
	}

	public int[] drawRowTopItems(PDDocument doc, PDPageContentStream contentStream, ImportData iData, int[] posArray) {
        try {
	       int colStartPos = posArray[PDF_COL_POS] + PDF_MAP_SIZE + 1;
	        BufferedImage bI = iData.rowData.topItemImage;
	        int matrixLen = iData.matrixFiles.get(0).reorgMatrix.length;  
	        PDImageXObject  pdImageClassXObjectR = LosslessFactory.createFromImage(doc, bI);
	        contentStream.drawImage(pdImageClassXObjectR, colStartPos, posArray[PDF_ROW_POS], PDF_CLASS_HEIGHT*2, PDF_MAP_SIZE);
	        Float increment = new Float(PDF_MAP_SIZE) / new Float(matrixLen-1);
	        int startRowPosition = posArray[PDF_ROW_POS] + PDF_MAP_SIZE;
	        for (int i = 0; i < iData.rowData.topItemsLines.size(); i++) {
		        String itemVal = (String) iData.rowData.topItemsLines.get(i)[2];
		        int textLoc = Math.round(((int)iData.rowData.topItemsLines.get(i)[1])*increment)+2 - (increment.intValue()/2);
		        if (itemVal != null) {
			        writePDFText(contentStream, itemVal, 5, PDF_FONT_BOLD, colStartPos+17, startRowPosition - textLoc, false);
		        }
	        }
		} catch (Exception ex) {
			System.out.println("Exception in PdfGenerator.drawRowTopItems: " + ex.toString());
	        ex.printStackTrace();
		} 
        return posArray;
	}
	
	public int[] drawAllRowLabels(PDDocument doc, PDPageContentStream contentStream, ImportData iData, int[] posArray, int mapWidth, int mapHeight) {
        try {
	       int colStartPos = posArray[PDF_COL_POS] + mapWidth + 1;
	        int startRowPosition = posArray[PDF_ROW_POS] + mapHeight;
	        for (int i = 0; i < iData.rowData.classArray.length; i++) {
		        String itemVal = iData.rowData.classArray[i];
		        if (itemVal != null) {
		    		int pipeIdx = itemVal.indexOf(PIPE);
		    		if (pipeIdx > 0) {
				        itemVal = itemVal.substring(0,itemVal.indexOf(PIPE));
		    		}
		        }
		        int textLoc = i*5 - 1;
		        if (itemVal != null && itemVal != CUT_VALUE) {
			        writePDFText(contentStream, itemVal, 5, PDF_FONT_BOLD, colStartPos, startRowPosition - textLoc, false);
		        }
	        }
		} catch (Exception ex) {
			System.out.println("Exception in PdfGenerator.drawRowTopItems: " + ex.toString());
	        ex.printStackTrace();
		} 
        return posArray;
	}
	
	public int[] drawColTopItems(PDDocument doc, PDPageContentStream contentStream, ImportData iData, int[] posArray) {
        try {
			int colStartPos = posArray[PDF_COL_POS];
	        int rowStartPos = (posArray[PDF_ROW_POS] - PDF_CLASS_HEIGHT*3) - 1;
	        int matrixLen = iData.matrixFiles.get(0).reorgMatrix[0].length;  
	        BufferedImage bI = iData.colData.topItemImage;
	        PDImageXObject  pdImageClassXObjectR = LosslessFactory.createFromImage(doc, bI);
	        contentStream.drawImage(pdImageClassXObjectR, colStartPos, rowStartPos, PDF_MAP_SIZE, PDF_CLASS_HEIGHT*3);
	        Float increment = new Float(PDF_MAP_SIZE) / new Float(matrixLen-1);
	        int startColPosition = posArray[PDF_COL_POS];
	        for (int i = 0; i < iData.colData.topItemsLines.size(); i++) {
		        String itemVal = (String) iData.colData.topItemsLines.get(i)[2];
		        int textLoc = Math.round(((int)iData.colData.topItemsLines.get(i)[1])*increment)-2 - (increment.intValue()/2);
		        if (itemVal != null) {
			        writePDFText(contentStream, itemVal, 5, PDF_FONT_BOLD, startColPosition+textLoc, rowStartPos-2, true);
		        } 
	        }
		} catch (Exception ex) {
			System.out.println("Exception in PdfGenerator.drawColTopItems: " + ex.toString());
	        ex.printStackTrace();
		} 
        return posArray;
	}
	
	public int[] drawAllColLabels(PDDocument doc, PDPageContentStream contentStream, ImportData iData, int[] posArray) {
        try {
	        int rowStartPos = (posArray[PDF_ROW_POS]) - 1;
	        int startColPosition = posArray[PDF_COL_POS];
	        for (int i = 0; i < iData.colData.classArray.length; i++) {
		        String itemVal = (String) iData.colData.classArray[i];
		        if (itemVal != null) {
		    		int pipeIdx = itemVal.indexOf(PIPE);
		    		if (pipeIdx > 0) {
				        itemVal = itemVal.substring(0,itemVal.indexOf(PIPE));
		    		}
		        }
		        int textLoc = Math.round((int)i*5)-5;
		        if (itemVal != null && itemVal != CUT_VALUE) {
			        writePDFText(contentStream, itemVal, 5, PDF_FONT_BOLD, startColPosition+textLoc, rowStartPos, true);
		        } 
	        }
		} catch (Exception ex) {
			System.out.println("Exception in PdfGenerator.drawColTopItems: " + ex.toString());
	        ex.printStackTrace();
		} 
        return posArray;
	}
	
	/*******************************************************************
	 * METHOD: writePDFText
	 *
	 * This is a helper method that writes text on the PDF page at a 
	 * given location.
	 ******************************************************************/
	public void writePDFText(PDPageContentStream contentStream, String text, int fontSize, PDFont font, int startX, int startY, boolean rotate) {
		try {
			contentStream.beginText();
			contentStream.setFont(PDF_FONT, fontSize);
			contentStream.newLineAtOffset(startX, startY);
			if (rotate) {
				contentStream.setTextRotation(-20.42, startX, startY);
			}
			contentStream.showText(text);
			contentStream.endText();
		} catch (Exception ex) {
			System.out.println("Exception in PdfGenerator.writePDFText: " + ex.toString());
	        ex.printStackTrace();
		} 
	}
}
