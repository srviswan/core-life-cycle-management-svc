-- SQL Server Script: Check Integer Columns Nearing Upper Limits
-- This script identifies integer columns that are approaching their maximum values
-- Performance optimized using dynamic SQL and system catalog views

SET NOCOUNT ON;

DECLARE @ThresholdPercent DECIMAL(5,2) = 10.0; -- Alert if within 10% of max value
DECLARE @MinRows INT = 100; -- Only check tables with at least this many rows (for performance)

-- Create temporary table to store results
IF OBJECT_ID('tempdb..#IntegerColumnResults') IS NOT NULL
    DROP TABLE #IntegerColumnResults;

CREATE TABLE #IntegerColumnResults (
    SchemaName NVARCHAR(128),
    TableName NVARCHAR(128),
    ColumnName NVARCHAR(128),
    DataType NVARCHAR(128),
    MaxValue BIGINT,
    UpperLimit BIGINT,
    CurrentPercent DECIMAL(10,4),
    RowCount BIGINT,
    IsNearLimit BIT,
    AlertLevel NVARCHAR(20)
);

-- Get all integer columns from system catalog
DECLARE @SQL NVARCHAR(MAX) = N'
INSERT INTO #IntegerColumnResults (SchemaName, TableName, ColumnName, DataType, UpperLimit, RowCount)
SELECT 
    s.name AS SchemaName,
    t.name AS TableName,
    c.name AS ColumnName,
    ty.name AS DataType,
    CASE 
        WHEN ty.name = ''TINYINT'' THEN 255
        WHEN ty.name = ''SMALLINT'' THEN 32767
        WHEN ty.name = ''INT'' THEN 2147483647
        WHEN ty.name = ''BIGINT'' THEN 9223372036854775807
    END AS UpperLimit,
    p.rows AS RowCount
FROM sys.columns c
INNER JOIN sys.tables t ON c.object_id = t.object_id
INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
INNER JOIN sys.types ty ON c.user_type_id = ty.user_type_id
INNER JOIN sys.partitions p ON t.object_id = p.object_id
WHERE ty.name IN (''TINYINT'', ''SMALLINT'', ''INT'', ''BIGINT'')
    AND p.index_id IN (0, 1) -- Heap or clustered index
    AND p.rows >= @MinRows
    AND t.is_ms_shipped = 0 -- Exclude system tables
ORDER BY s.name, t.name, c.name;
';

EXEC sp_executesql @SQL, N'@MinRows INT', @MinRows = @MinRows;

-- Now check max values for each column (using dynamic SQL for performance)
DECLARE @SchemaName NVARCHAR(128);
DECLARE @TableName NVARCHAR(128);
DECLARE @ColumnName NVARCHAR(128);
DECLARE @FullTableName NVARCHAR(500);
DECLARE @CheckSQL NVARCHAR(MAX);
DECLARE @MaxValue BIGINT;

DECLARE col_cursor CURSOR FAST_FORWARD FOR
SELECT SchemaName, TableName, ColumnName, 
       QUOTENAME(SchemaName) + '.' + QUOTENAME(TableName) AS FullTableName
FROM #IntegerColumnResults
ORDER BY SchemaName, TableName, ColumnName;

OPEN col_cursor;
FETCH NEXT FROM col_cursor INTO @SchemaName, @TableName, @ColumnName, @FullTableName;

WHILE @@FETCH_STATUS = 0
BEGIN
    -- Build dynamic SQL to get max value
    SET @CheckSQL = N'
    SELECT @MaxValue = MAX(CAST(' + QUOTENAME(@ColumnName) + ' AS BIGINT))
    FROM ' + @FullTableName + '
    WHERE ' + QUOTENAME(@ColumnName) + ' IS NOT NULL;
    ';
    
    SET @MaxValue = NULL;
    
    BEGIN TRY
        EXEC sp_executesql @CheckSQL, N'@MaxValue BIGINT OUTPUT', @MaxValue = @MaxValue OUTPUT;
        
        -- Update results with max value
        UPDATE #IntegerColumnResults
        SET MaxValue = ISNULL(@MaxValue, 0),
            CurrentPercent = CASE 
                WHEN UpperLimit > 0 THEN (CAST(ISNULL(@MaxValue, 0) AS DECIMAL(20,4)) / CAST(UpperLimit AS DECIMAL(20,4))) * 100.0
                ELSE 0
            END,
            IsNearLimit = CASE 
                WHEN UpperLimit > 0 AND (CAST(ISNULL(@MaxValue, 0) AS DECIMAL(20,4)) / CAST(UpperLimit AS DECIMAL(20,4))) * 100.0 >= (100.0 - @ThresholdPercent)
                THEN 1
                ELSE 0
            END,
            AlertLevel = CASE
                WHEN UpperLimit > 0 AND (CAST(ISNULL(@MaxValue, 0) AS DECIMAL(20,4)) / CAST(UpperLimit AS DECIMAL(20,4))) * 100.0 >= 99.0 THEN 'CRITICAL'
                WHEN UpperLimit > 0 AND (CAST(ISNULL(@MaxValue, 0) AS DECIMAL(20,4)) / CAST(UpperLimit AS DECIMAL(20,4))) * 100.0 >= (100.0 - @ThresholdPercent) THEN 'WARNING'
                ELSE 'OK'
            END
        WHERE SchemaName = @SchemaName
            AND TableName = @TableName
            AND ColumnName = @ColumnName;
    END TRY
    BEGIN CATCH
        -- Skip columns that can't be queried (permissions, etc.)
        UPDATE #IntegerColumnResults
        SET MaxValue = -1,
            AlertLevel = 'ERROR'
        WHERE SchemaName = @SchemaName
            AND TableName = @TableName
            AND ColumnName = @ColumnName;
    END CATCH
    
    FETCH NEXT FROM col_cursor INTO @SchemaName, @TableName, @ColumnName, @FullTableName;
END

CLOSE col_cursor;
DEALLOCATE col_cursor;

-- Display results
PRINT '================================================================================';
PRINT 'INTEGER COLUMNS NEARING UPPER LIMITS';
PRINT '================================================================================';
PRINT 'Threshold: ' + CAST(@ThresholdPercent AS VARCHAR(10)) + '% from maximum';
PRINT 'Minimum Rows: ' + CAST(@MinRows AS VARCHAR(10));
PRINT '';

-- Summary
SELECT 
    COUNT(*) AS TotalIntegerColumns,
    SUM(CASE WHEN IsNearLimit = 1 THEN 1 ELSE 0 END) AS ColumnsNearLimit,
    SUM(CASE WHEN AlertLevel = 'CRITICAL' THEN 1 ELSE 0 END) AS CriticalColumns,
    SUM(CASE WHEN AlertLevel = 'WARNING' THEN 1 ELSE 0 END) AS WarningColumns
FROM #IntegerColumnResults;

PRINT '';
PRINT '================================================================================';
PRINT 'DETAILED RESULTS - COLUMNS NEARING LIMITS (Sorted by Alert Level)';
PRINT '================================================================================';

-- Detailed results - only show columns that need attention
SELECT 
    SchemaName + '.' + TableName AS [Table],
    ColumnName AS [Column],
    DataType AS [Type],
    FORMAT(MaxValue, 'N0') AS [Max Value],
    FORMAT(UpperLimit, 'N0') AS [Upper Limit],
    FORMAT(CurrentPercent, 'N2') + '%' AS [% of Limit],
    FORMAT(RowCount, 'N0') AS [Row Count],
    AlertLevel AS [Alert Level],
    CASE 
        WHEN UpperLimit - MaxValue < 1000 THEN 'IMMEDIATE ACTION REQUIRED'
        WHEN UpperLimit - MaxValue < 10000 THEN 'ACTION REQUIRED SOON'
        ELSE 'MONITOR'
    END AS [Recommendation]
FROM #IntegerColumnResults
WHERE IsNearLimit = 1 OR AlertLevel IN ('CRITICAL', 'WARNING', 'ERROR')
ORDER BY 
    CASE AlertLevel
        WHEN 'CRITICAL' THEN 1
        WHEN 'WARNING' THEN 2
        WHEN 'ERROR' THEN 3
        ELSE 4
    END,
    CurrentPercent DESC,
    SchemaName,
    TableName,
    ColumnName;

PRINT '';
PRINT '================================================================================';
PRINT 'ALL INTEGER COLUMNS (Full Report)';
PRINT '================================================================================';

-- Full report of all integer columns
SELECT 
    SchemaName + '.' + TableName AS [Table],
    ColumnName AS [Column],
    DataType AS [Type],
    CASE 
        WHEN MaxValue = -1 THEN 'ERROR'
        ELSE FORMAT(MaxValue, 'N0')
    END AS [Max Value],
    FORMAT(UpperLimit, 'N0') AS [Upper Limit],
    CASE 
        WHEN MaxValue = -1 THEN 'N/A'
        ELSE FORMAT(CurrentPercent, 'N2') + '%'
    END AS [% of Limit],
    FORMAT(UpperLimit - ISNULL(MaxValue, 0), 'N0') AS [Remaining Capacity],
    FORMAT(RowCount, 'N0') AS [Row Count],
    AlertLevel AS [Alert Level]
FROM #IntegerColumnResults
ORDER BY 
    CASE AlertLevel
        WHEN 'CRITICAL' THEN 1
        WHEN 'WARNING' THEN 2
        WHEN 'ERROR' THEN 3
        ELSE 4
    END,
    CurrentPercent DESC,
    SchemaName,
    TableName,
    ColumnName;

-- Cleanup
DROP TABLE #IntegerColumnResults;

PRINT '';
PRINT '================================================================================';
PRINT 'Script completed successfully.';
PRINT '================================================================================';
