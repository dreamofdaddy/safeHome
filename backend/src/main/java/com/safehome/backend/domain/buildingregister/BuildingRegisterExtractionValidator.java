package com.safehome.backend.domain.buildingregister;

import static com.safehome.backend.domain.buildingregister.BuildingRegisterModels.*;
import com.safehome.backend.domain.document.parser.RegistryParseException;
import java.util.*;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import java.awt.geom.Rectangle2D;

/** Conservative source checks; PDF text is data and never executable instructions. */
public final class BuildingRegisterExtractionValidator {
    private BuildingRegisterExtractionValidator() {}
    public record Source(Set<String> documentTypes,int pages) {}
    private static final Pattern HEADING=Pattern.compile("(?:집합)?건축물대장\\((전유부|표제부),[갑을]\\)|(?:건축물대장)?총괄표제부\\([갑을]\\)|일반건축물대장\\([갑을]\\)");
    public static List<Source> inspect(List<byte[]> pdfs) {
        var sources=new ArrayList<Source>();
        try {
            for(var bytes:pdfs) try(var document=Loader.loadPDF(bytes)) {
                var kinds=new HashSet<String>();
                for(int page=1;page<=document.getNumberOfPages();page++) {
                    var sourcePage=document.getPage(page-1);var box=sourcePage.getCropBox();
                    var stripper=new PDFTextStripperByArea();stripper.setSortByPosition(true);
                    stripper.addRegion("heading",new Rectangle2D.Double(0,0,box.getWidth(),box.getHeight()*0.2));
                    stripper.extractRegions(sourcePage);
                    String text=stripper.getTextForRegion("heading").replaceAll("\\s","");
                    var matcher=HEADING.matcher(text.substring(0,Math.min(text.length(),600)));
                    while(matcher.find()) {
                        String heading=matcher.group();
                        kinds.add(heading.contains("전유부")?"EXCLUSIVE":heading.contains("총괄표제부")?"SUMMARY":heading.startsWith("일반")?"GENERAL":"TITLE");
                    }
                }
                sources.add(new Source(Set.copyOf(kinds),document.getNumberOfPages()));
            }
            return List.copyOf(sources);
        } catch(Exception e) {throw new RegistryParseException("PARSER_INVALID_SOURCE");}
    }
    public static void validateCoverage(Extraction extraction,int fileCount) {
        if(extraction==null || !extraction.buildingRegisterDocument()) return;
        if(extraction.entries()==null || extraction.entries().isEmpty()) throw new RegistryParseException("PARSER_INCOMPLETE");
        var indices=new HashSet<Integer>();
        for(var entry:extraction.entries()) {
            if(entry==null || entry.fileIndex()<1 || entry.fileIndex()>fileCount) throw new RegistryParseException("PARSER_INVALID_OUTPUT");
            indices.add(entry.fileIndex());
        }
        if(indices.size()!=fileCount) throw new RegistryParseException("PARSER_INCOMPLETE");
    }
    public static void validate(Extraction extraction,List<Source> sources) {
        validateCoverage(extraction,sources.size());
        if(extraction==null || !extraction.buildingRegisterDocument()) return;
        for(var entry:extraction.entries()) {
            var source=sources.get(entry.fileIndex()-1);
            if(entry.page()<1 || entry.page()>source.pages() || entry.evidence()==null) throw new RegistryParseException("PARSER_INVALID_OUTPUT");
            if(!source.documentTypes().isEmpty() && !source.documentTypes().contains(entry.documentType()))
                throw new RegistryParseException("PARSER_SOURCE_MISMATCH");
            for(var note:entry.evidence()) if(note==null || note.page()<1 || note.page()>source.pages())
                throw new RegistryParseException("PARSER_INVALID_OUTPUT");
        }
    }
}
