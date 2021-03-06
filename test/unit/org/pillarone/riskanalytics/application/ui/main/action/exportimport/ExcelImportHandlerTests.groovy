package org.pillarone.riskanalytics.application.ui.main.action.exportimport

import models.application.ApplicationModel
import models.core.CoreModel
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.xssf.usermodel.XSSFCell
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.joda.time.DateTime
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.pillarone.riskanalytics.application.example.component.ExampleDynamicComponent
import org.pillarone.riskanalytics.application.example.component.ExampleInputOutputComponentWithSubcomponent
import org.pillarone.riskanalytics.application.util.LocaleResources
import org.pillarone.riskanalytics.core.components.Component
import org.pillarone.riskanalytics.core.example.parameter.ExampleResourceConstraints
import org.pillarone.riskanalytics.core.parameterization.ConstraintsFactory

class ExcelImportHandlerTests {
    File exportFile
    TimeZone oldOne

    @Before
    void setUp() throws Exception {
        oldOne = TimeZone.default
        TimeZone.default = TimeZone.getTimeZone("UTC")
        LocaleResources.testMode = true
        ConstraintsFactory.registerConstraint(new ExampleResourceConstraints())
        exportFile = File.createTempFile('excel', '.xlsx')
        exportFile.bytes = new ExcelExportHandler(new ApplicationModel()).exportModel()
    }

    @After
    void tearDown() throws Exception {
        TimeZone.default = oldOne
        LocaleResources.testMode = false
    }

    @Test
    void testIncorrectModelClass() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        handler.workbook.getSheet(AbstractExcelHandler.META_INFO_SHEET).getRow(0).getCell(1).setCellValue(CoreModel.class.name)
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
    }

    @Test
    void testInvalidModelClass() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        handler.workbook.getSheet(AbstractExcelHandler.META_INFO_SHEET).getRow(0).getCell(1).setCellValue("NONEXISTINGCLASS")
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
    }

    @Test
    void testModelInfoNotFound() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        handler.workbook.getSheet(AbstractExcelHandler.META_INFO_SHEET).getRow(0).getCell(1).setCellValue(null as String)
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
    }

    @Test
    void testMissingMetaInfoSheet() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        int sheetIndex = handler.workbook.getSheetIndex(AbstractExcelHandler.META_INFO_SHEET)
        handler.workbook.removeSheetAt(sheetIndex)
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
    }

    @Test
    void testMissingMDPSheet() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet parmComponentSheet = handler.workbook.getSheet('parameterComponent')
        XSSFRow dataRow = parmComponentSheet.createRow(2)
        dataRow.createCell(2).setCellValue('RESOURCE')
        dataRow.createCell(1).setCellValue('FIRST_VALUE')
        dataRow.createCell(10).setCellValue('tableName')
        int sheetIndex = handler.workbook.getSheetIndex('MDP0-ExampleResourceConstraints')
        handler.workbook.removeSheetAt(sheetIndex)
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
    }

    @Test
    void testMissingMDPTableReference() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet parmComponentSheet = handler.workbook.getSheet('parameterComponent')
        XSSFRow dataRow = parmComponentSheet.createRow(2)
        dataRow.createCell(1).setCellValue('FIRST_VALUE')
        dataRow.createCell(2).setCellValue('RESOURCE')
        dataRow.createCell(10).setCellValue('tableName')
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
    }

    @Test
    void testMissingMDPTableIdentifier() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet parmComponentSheet = handler.workbook.getSheet('parameterComponent')
        XSSFRow dataRow = parmComponentSheet.createRow(2)
        dataRow.createCell(2).setCellValue('RESOURCE')
        dataRow.createCell(1).setCellValue('FIRST_VALUE')
        dataRow.createCell(10).setCellValue('')
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
    }

    @Test
    void testUnknownEnumDisplayName() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet parmComponentSheet = handler.workbook.getSheet('parameterComponent')
        XSSFRow dataRow = parmComponentSheet.createRow(2)
        dataRow.createCell(2).setCellValue('TYPE0')
        dataRow.createCell(1).setCellValue('unknown')
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
    }

    @Test
    void testEnumDisplayName() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet parmComponentSheet = handler.workbook.getSheet('parameterComponent')
        XSSFRow dataRow = parmComponentSheet.createRow(2)
        dataRow.createCell(2).setCellValue('TYPE0')
        dataRow.createCell(1).setCellValue('First value')
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 0 == result.size()
    }

    @Test
    void testEnumTechnicalName() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet parmComponentSheet = handler.workbook.getSheet('parameterComponent')
        XSSFRow dataRow = parmComponentSheet.createRow(2)
        dataRow.createCell(2).setCellValue('TYPE0')
        dataRow.createCell(1).setCellValue('FIRST_VALUE')
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 0 == result.size()
    }

    @Test
    void testIncorrectParameterObjectClassifier() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet parmComponentSheet = handler.workbook.getSheet('parameterComponent')
        XSSFRow dataRow = parmComponentSheet.createRow(2)
        dataRow.createCell(2).setCellValue('UNKNOWN_CLASSIFIER')
        dataRow.createCell(1).setCellValue('FIRST_VALUE')
        dataRow.createCell(10).setCellValue('')
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
    }

    @Test
    void testMDPValues() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet parmComponentSheet = handler.workbook.getSheet('parameterComponent')
        XSSFRow dataRow = parmComponentSheet.createRow(2)
        dataRow.createCell(2).setCellValue('RESOURCE')
        dataRow.createCell(10).setCellValue('tableName')
        XSSFSheet mdpSheet = handler.workbook.getSheet('MDP0-ExampleResourceConstraints')
        mdpSheet.getRow(0).getCell(0).setCellValue('tableName')
        XSSFRow mdpRow = mdpSheet.createRow(2)
        mdpRow.createCell(0).setCellValue('ONE')
        mdpRow.createCell(1).setCellValue('BESIDE')
        mdpRow = mdpSheet.createRow(3)
        mdpRow.createCell(0).setCellValue('TWO')
        mdpRow.createCell(1).setCellValue('BESIDE')
        mdpRow = mdpSheet.createRow(4)
        mdpRow.createCell(0).setCellValue('')
        mdpRow.createCell(1).setCellValue('BESIDE')
        handler.validate(new ApplicationModel())
        assert ['ONE', 'TWO'] == handler.modelInstance.parameterComponent.parmNestedMdp.parameters['resource'].values[0]
    }

    @Test
    void testMDPEmptyValues_EmptyString() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet parmComponentSheet = handler.workbook.getSheet('parameterComponent')
        XSSFRow dataRow = parmComponentSheet.createRow(2)
        dataRow.createCell(2).setCellValue('RESOURCE')
        dataRow.createCell(10).setCellValue('tableName')
        XSSFSheet mdpSheet = handler.workbook.getSheet('MDP0-ExampleResourceConstraints')
        mdpSheet.getRow(0).getCell(0).setCellValue('tableName')
        XSSFRow mdpRow = mdpSheet.createRow(2)
        mdpRow.createCell(0).setCellValue('ONE')
        mdpRow = mdpSheet.createRow(3)
        mdpRow.createCell(0).setCellValue('TWO')
        mdpRow = mdpSheet.createRow(4)
        mdpRow.createCell(0).setCellValue('')
        handler.validate(new ApplicationModel())
        assert ['ONE', 'TWO'] == handler.modelInstance.parameterComponent.parmNestedMdp.parameters['resource'].values[0]
    }

    @Test
    void testMDPEmptyValues_FormulaString() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet parmComponentSheet = handler.workbook.getSheet('parameterComponent')
        XSSFRow dataRow = parmComponentSheet.createRow(2)
        dataRow.createCell(2).setCellValue('RESOURCE')
        dataRow.createCell(10).setCellValue('tableName')
        XSSFSheet mdpSheet = handler.workbook.getSheet('MDP0-ExampleResourceConstraints')
        mdpSheet.getRow(0).getCell(0).setCellValue('tableName')
        XSSFRow mdpRow = mdpSheet.createRow(2)
        mdpRow.createCell(0).setCellValue('ONE')
        mdpRow = mdpSheet.createRow(3)
        mdpRow.createCell(0).setCellValue('TWO')
        mdpRow = mdpSheet.createRow(4)
        Cell cell = mdpRow.createCell(0)
        cell.setCellType(Cell.CELL_TYPE_FORMULA)
        cell.setCellFormula('""')
        handler.validate(new ApplicationModel())
        assert ['ONE', 'TWO'] == handler.modelInstance.parameterComponent.parmNestedMdp.parameters['resource'].values[0]
    }

    @Test
    void testIncorrectCellData() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet sheet = handler.workbook.getSheet('parameterComponent')
        XSSFRow dataRow = sheet.createRow(2)
        dataRow.createCell(1).setCellValue('DUMMY')
        dataRow.createCell(2).setCellValue('TYPE0')
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
    }

    @Test
    void testMissingCellData() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet sheet = handler.workbook.getSheet('parameterComponent')
        XSSFRow dataRow = sheet.createRow(2)
        dataRow.createCell(2).setCellValue('TYPE0')
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
        assert result[0].toString().contains('Col=B')
    }

    @Test
    void testAddSubComponent() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet sheet = handler.workbook.getSheet('dynamicComponent')
        XSSFRow dataRow = sheet.createRow(2)
        dataRow.createCell(1).setCellValue('DUMMY')
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
    }

    @Test
    void testDisableImport() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet sheet = handler.workbook.getSheet('dynamicComponent')
        XSSFRow dataRow = sheet.createRow(2)
        dataRow.createCell(0).setCellValue('#')
        dataRow.createCell(1).setCellValue('componentA')
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 0 == result.size()
    }

    @Test
    void testDisableImport_invalidFormatting() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet sheet = handler.workbook.getSheet('dynamicComponent')
        XSSFRow dataRow = sheet.createRow(2)
        Cell cell = dataRow.createCell(0)
        cell.setCellType(Cell.CELL_TYPE_NUMERIC)
        cell.setCellValue(1)
        dataRow.createCell(1).setCellValue('componentA')
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
        assert "Component 'componentA' processed." == result[0].message

    }

    @Test
    void testDisableImport_Formula() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet sheet = handler.workbook.getSheet('dynamicComponent')
        XSSFRow dataRow = sheet.createRow(2)
        Cell cell = dataRow.createCell(0)
        cell.setCellType(Cell.CELL_TYPE_FORMULA)
        cell.setCellFormula('IF(TRUE,"#","")')
        dataRow.createCell(1).setCellValue('componentA')
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 0 == result.size()
        cell.setCellFormula('IF(TRUE,TRUE,FALSE)')
        result = handler.validate(new ApplicationModel())
        assert 0 == result.size()
        cell.setCellFormula('IF(TRUE,FALSE,TRUE)')
        result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
    }

    @Test
    void testDisableImport_booleanCell() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet sheet = handler.workbook.getSheet('dynamicComponent')
        XSSFRow dataRow = sheet.createRow(2)
        Cell cell = dataRow.createCell(0, Cell.CELL_TYPE_BOOLEAN)
        cell.setCellValue(true)
        dataRow.createCell(1).setCellValue('componentA')
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 0 == result.size()
        cell.setCellValue(false)
        result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
    }

    @Test
    void testImportExistingComponent() {
        ApplicationModel model = new ApplicationModel()
        model.init()
        model.injectComponentNames()
        ExampleDynamicComponent dynamicComponent = model.dynamicComponent
        Component subComponent = dynamicComponent.createDefaultSubComponent()
        subComponent.name = 'subComponentA'
        dynamicComponent.addSubComponent(subComponent)

        ExcelImportHandler handler = new ExcelImportHandler()

        handler.parameterizedModel = model
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet sheet = handler.workbook.getSheet('dynamicComponent')
        XSSFRow dataRow = sheet.createRow(2)
        dataRow.createCell(1).setCellValue('componentA')
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
        assert "Parameterization for component 'componentA' already present. Will be ignored." == result[0].message
    }

    @Test
    void testImportToNonExistingComponent() {
        ApplicationModel model = new ApplicationModel()
        model.init()
        model.injectComponentNames()
        ExampleDynamicComponent dynamicComponent = model.dynamicComponent
        Component subComponent = dynamicComponent.createDefaultSubComponent()
        subComponent.name = 'subComponentA'
        dynamicComponent.addSubComponent(subComponent)

        ExcelImportHandler handler = new ExcelImportHandler()

        handler.parameterizedModel = model
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet sheet = handler.workbook.getSheet('dynamicComponent')
        XSSFRow dataRow = sheet.createRow(2)
        dataRow.createCell(1).setCellValue('componentB')
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
        assert "Component 'componentB' processed." == result[0].message
    }

    @Test
    void testImportDateFunction() {
        int year = 2012
        int day = 23
        int month = 1
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet sheet = handler.workbook.getSheet('globalParameterComponent')
        XSSFRow dataRow = sheet.createRow(2)
        XSSFCell cell = dataRow.createCell(0)
        cell.cellType = Cell.CELL_TYPE_FORMULA
        cell.cellFormula = "DATE($year,$month,$day)"
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 0 == result.size()
        DateTime date = (handler.modelInstance as ApplicationModel).globalParameterComponent.parmProjectionStartDate
        assert year == date.getYear()
        assert month == date.getMonthOfYear()
        assert day == date.getDayOfMonth()
    }

    @Test
    void testInstantiationErrorForClassifier() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), 'test.xlsx')
        XSSFSheet sheet = handler.workbook.getSheet('parameterComponent')
        XSSFRow dataRow = sheet.createRow(2)
        dataRow.createCell(1).setCellValue('FIRST_VALUE')
        dataRow.createCell(2).setCellValue('TYPE_WITH_ERROR')
        List result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
    }

    @Test
    void importTwoDynamicComponentWithSameName() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet sheet = handler.workbook.getSheet('dynamicComponent')
        XSSFRow dataRow = sheet.createRow(2)
        dataRow.createCell(1).setCellValue('DUMMY')
        dataRow = sheet.createRow(3)
        dataRow.createCell(1).setCellValue('DUMMY')
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 2 == result.size()
        assert 1 == result.findAll { it.type == ImportResult.Type.WARNING }.size()
    }

    @Test
    void mdpNameIsNumericValue() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet parmComponentSheet = handler.workbook.getSheet('parameterComponent')
        XSSFRow dataRow = parmComponentSheet.createRow(2)
        dataRow.createCell(1).setCellValue('FIRST_VALUE')
        dataRow.createCell(2).setCellValue('RESOURCE')
        Cell cell = dataRow.createCell(10, Cell.CELL_TYPE_NUMERIC)
        cell.setCellValue(20)
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 2 == result.size()
        cell = dataRow.createCell(10, Cell.CELL_TYPE_FORMULA)
        cell.setCellFormula('IF(TRUE,20,-1)')
        result = handler.validate(new ApplicationModel())
        assert 2 == result.size()
    }

    @Test
    void importBooleanCell() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet sheet = handler.workbook.getSheet('globalParameterComponent')
        XSSFRow dataRow = sheet.createRow(2)
        XSSFCell cell = dataRow.createCell(1, Cell.CELL_TYPE_BOOLEAN)
        cell.setCellValue(true)
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 0 == result.size()
        ApplicationModel applicationModel = handler.modelInstance as ApplicationModel
        assert applicationModel.globalParameterComponent.parmRunOffAfterFirstPeriod
    }

    @Test
    void importMultipleSubComponent() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet sheet = handler.workbook.getSheet('dynamicComponent')
        XSSFRow dataRow = sheet.createRow(2)
        dataRow.createCell(1).setCellValue('DUMMY')
        dataRow.createCell(2).setCellValue(22)
        dataRow.createCell(5).setCellValue(1000)
        dataRow = sheet.createRow(3)
        dataRow.createCell(1).setCellValue('DUMMY2')
        dataRow.createCell(2).setCellValue(44)
        dataRow.createCell(5).setCellValue(100)
        List<ImportResult> result = handler.validate(new ApplicationModel())
        ApplicationModel applicationModel = handler.modelInstance as ApplicationModel
        ExampleInputOutputComponentWithSubcomponent dummyComp = applicationModel.dynamicComponent.getComponentByName('subDUMMY')
        ExampleInputOutputComponentWithSubcomponent dummy2Comp = applicationModel.dynamicComponent.getComponentByName('subDUMMY2')
        assert dummyComp
        assert dummy2Comp
        assert 1000 == dummyComp.subSecondComponent.parmValue
        assert 22 == dummyComp.parmFirstParameter
        assert 100 == dummy2Comp.subSecondComponent.parmValue
        assert 44 == dummy2Comp.parmFirstParameter
    }

    @Test
    void subComponentMissing() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet parmComponentSheet = handler.workbook.getSheet('parameterComponent')
        XSSFRow dataRow = parmComponentSheet.createRow(2)
        dataRow.createCell(2).setCellValue('TYPE0')
        dataRow.createCell(1).setCellValue('FIRST_VALUE')
        dataRow.createCell(11).setCellValue('abc component')
        def validate = handler.validate(new ApplicationModel())
        assert 1 == validate.size()
        assert 'Cell must reference to an existing component.' == validate[0].message
    }

    @Test
    void subComponentExist() {
        String componentName = 'abc component'
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet parmComponentSheet = handler.workbook.getSheet('parameterComponent')
        XSSFRow dataRow = parmComponentSheet.createRow(2)
        dataRow.createCell(2).setCellValue('TYPE0')
        dataRow.createCell(1).setCellValue('FIRST_VALUE')
        dataRow.createCell(11).setCellValue(componentName)
        XSSFSheet dynamicComponentSheet = handler.workbook.getSheet('dynamicComponent')
        dataRow = dynamicComponentSheet.createRow(2)
        dataRow.createCell(1).setCellValue(componentName)
        def validate = handler.validate(new ApplicationModel())
        assert 1 == validate.size()
        assert "Component '$componentName' processed." == validate[0].message

    }

    @Test
    void unknownFormula() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet dynamicComponentSheet = handler.workbook.getSheet('dynamicComponent')
        XSSFRow dataRow = dynamicComponentSheet.createRow(2)
        dataRow.createCell(1, Cell.CELL_TYPE_FORMULA).setCellFormula('COUNTIFS(A1,1)')
        def validate = handler.validate(new ApplicationModel())
        assert 2 == validate.size()
        assert "Formula not implemented: 'COUNTIFS'" == validate[0].message
    }
}