package com.JUtils.dataBase;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLColumnDefinition;
import com.alibaba.druid.sql.dialect.mysql.ast.MySqlPrimaryKey;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlCreateTableStatement;
import com.alibaba.druid.util.JdbcConstants;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SqlToWord {
    public static void main(String[] args) {
        SqlToWord sqlToWord = new SqlToWord();
        String result = sqlToWord.fileReader("C:\\Users\\yue.ren\\Desktop\\*.sql");
        List<Table> tableList = sqlToWord.getItem(result);
        sqlToWord.toExcel(tableList);
    }

    public String fileReader(String filePath) {
        try {
            FileReader fileReader = new FileReader(filePath);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            StringBuilder stringBuilder = new StringBuilder();
            bufferedReader.lines().forEach(line->{
                if (line.startsWith("--")){
                    System.out.println(line);
                    return;
                }
                stringBuilder.append(line);
            });
            return stringBuilder.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    public List<Table> getItem(String sql) {
        List<Table> tableList = new LinkedList<>();
        String[] strings = sql.split(";");
        for (int i = 0; i < strings.length; i++) {
            sql = strings[i];
            List<SQLStatement> sqlStatements = new LinkedList<>();

            try {
                sqlStatements = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
            } catch (Exception e) {
                System.out.println("第" + i + "张表需要去掉注释");
                continue;
            }
            sqlStatements.forEach(sqlStatement -> {
                Table table = new Table();
                List<Column> columnList = new LinkedList<>();
                MySqlCreateTableStatement mySqlCreateTableStatement = (MySqlCreateTableStatement) sqlStatement;
                mySqlCreateTableStatement.getTableElementList().forEach(sqlTableElement -> {
                    Column column = new Column();
                    if (sqlTableElement instanceof SQLColumnDefinition) {
                        SQLColumnDefinition sqlColumnDefinition = ((SQLColumnDefinition) sqlTableElement);
                        column.setComment(sqlColumnDefinition.getComment().toString());
                        column.setName(sqlColumnDefinition.getNameAsString());
                        column.setType(sqlColumnDefinition.getDataType().toString());
                        //  column.setNull(sqlColumnDefinition.getValidate());
                    } else if (sqlTableElement instanceof MySqlPrimaryKey) {
                        //todo 主键索引
                        return;
                    }
                    columnList.add(column);
                });
                table.setColumns(columnList);
                table.setTableName(((MySqlCreateTableStatement) sqlStatement).getName().toString());
                table.setComment(((MySqlCreateTableStatement) sqlStatement).getComment().toString());
                tableList.add(table);
            });
        }

        return tableList;
    }

    public void toExcel(List<Table> tables) {
        HSSFWorkbook hssfWorkbook = new HSSFWorkbook();
        HSSFSheet hssfSheet = hssfWorkbook.createSheet("表结构");
        AtomicInteger index = new AtomicInteger();
        tables.forEach(table -> {
            HSSFRow row = hssfSheet.createRow(index.get());
            row.createCell(0).setCellValue(table.getTableName());
            CellRangeAddress cellAddresses = new CellRangeAddress(index.get(), index.get(), 0, 3);
            hssfSheet.addMergedRegion(cellAddresses);
            index.getAndIncrement();
            for (int i = 0; i < table.getColumns().size(); i++) {
                Column column = table.getColumns().get(i);
                if (0 == i) {
                    HSSFRow hssfRow = hssfSheet.createRow(index.get());
                    hssfRow.createCell(0).setCellValue("字段名");
                    hssfRow.createCell(1).setCellValue("类型");
                    hssfRow.createCell(2).setCellValue("COMMENT");
                    hssfRow.createCell(3).setCellValue("非空");
                    hssfRow = hssfSheet.createRow(index.get() + 1);
                    hssfRow.createCell(0).setCellValue(column.getName());
                    hssfRow.createCell(1).setCellValue(column.getType());
                    hssfRow.createCell(2).setCellValue(column.getComment());
                    hssfRow.createCell(3).setCellValue(column.isNull() ? "Y" : "N");
                } else {
                    HSSFRow hssfRow = hssfSheet.createRow(index.get());
                    hssfRow.createCell(0).setCellValue(column.getName());
                    hssfRow.createCell(1).setCellValue(column.getType());
                    hssfRow.createCell(2).setCellValue(column.getComment());
                    hssfRow.createCell(3).setCellValue(column.isNull() ? "Y" : "N");
                }
                index.getAndIncrement();
            }
        });
        try {
            hssfWorkbook.write(new File("C:\\Users\\yue.ren\\Desktop\\文档.xls"));
            hssfWorkbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
