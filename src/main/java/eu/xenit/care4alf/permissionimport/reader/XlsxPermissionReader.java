package eu.xenit.care4alf.permissionimport.reader;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class XlsxPermissionReader implements PermissionReader {

    private final XSSFWorkbook xssfWorkbook;
    private final InputStream inputStream;

    private static final Logger LOG = LoggerFactory.getLogger(XlsxPermissionReader.class);


    public XlsxPermissionReader(InputStream inputStream) throws IOException {
        this.inputStream = inputStream;
        this.xssfWorkbook = new XSSFWorkbook(inputStream);
    }

    @Override
    public Iterator<PermissionSetting> iterator() {
        return new Iterator<PermissionSetting>() {

            private XSSFSheet xssfSheet = xssfWorkbook.getSheetAt(0);

            @Override
            public boolean hasNext() {
                return next!=null;
            }

            private int rowNumber = 1;
            private PermissionSetting next;
            {
                populateNext();
            }


            private void populateNext(){
                LOG.debug("Populating next of Iterator");
                if(rowNumber <= xssfSheet.getLastRowNum()){
                    populateNextInSheet();
                } else {
                    LOG.debug("Reached end of Excel file");
                    next = null;
                }

                rowNumber ++;
            }

            private void populateNextInSheet() {
                XSSFRow row = xssfSheet.getRow(rowNumber);
                next = new PermissionSetting();
                for (Iterator<Cell> it = row.iterator(); it.hasNext(); ) {
                    Cell cell = it.next();
                    if(cell.getColumnIndex() == 2){
                        String pathString = cell.getStringCellValue().trim();
                        // cut of slash in the beginning if needed, otherwise empty first part of path
                        if(pathString.charAt(0) == '/'){
                            next.setPath(pathString.substring(1).split("/"));
                        } else {
                            next.setPath(pathString.split("/"));
                        }
                        LOG.debug("Path: " + Arrays.toString(next.getPath()));
                    }
                    if(cell.getColumnIndex() == 0){
                        next.setGroup(cell.getStringCellValue().trim());
                        LOG.debug("Group: " + next.getGroup());
                    }
                    if(cell.getColumnIndex() == 1){
                        next.setPermission(cell.getStringCellValue().trim());
                        LOG.debug("Permission: " + next.getPermission());
                    }
                    if(cell.getColumnIndex() == 3){
                        next.setInherit(cell.getBooleanCellValue());
                        LOG.debug("Inheritance: " + next.isInherit());
                    }
                }

            }

            @Override
            public PermissionSetting next() {
                if(next == null){
                    throw new NoSuchElementException("There is no next element");
                } else {
                    LOG.debug("Returning next element");
                    PermissionSetting result = next;
                    populateNext();
                    return result;
                    //some logic to check preread the next element
                }
            }

            @Override
            public void remove() {
                throw new NotImplementedException("Remove method not implemented");
            }
        };
    }
}
