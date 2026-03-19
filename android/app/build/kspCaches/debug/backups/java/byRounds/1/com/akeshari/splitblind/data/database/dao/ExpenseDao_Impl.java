package com.akeshari.splitblind.data.database.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.akeshari.splitblind.data.database.entity.ExpenseEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ExpenseDao_Impl implements ExpenseDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ExpenseEntity> __insertionAdapterOfExpenseEntity;

  public ExpenseDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfExpenseEntity = new EntityInsertionAdapter<ExpenseEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `expenses` (`expenseId`,`groupId`,`description`,`amountCents`,`currency`,`paidBy`,`splitAmong`,`createdAt`,`isDeleted`,`hlcTimestamp`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ExpenseEntity entity) {
        statement.bindString(1, entity.getExpenseId());
        statement.bindString(2, entity.getGroupId());
        statement.bindString(3, entity.getDescription());
        statement.bindLong(4, entity.getAmountCents());
        statement.bindString(5, entity.getCurrency());
        statement.bindString(6, entity.getPaidBy());
        statement.bindString(7, entity.getSplitAmong());
        statement.bindLong(8, entity.getCreatedAt());
        final int _tmp = entity.isDeleted() ? 1 : 0;
        statement.bindLong(9, _tmp);
        statement.bindLong(10, entity.getHlcTimestamp());
      }
    };
  }

  @Override
  public Object insertExpense(final ExpenseEntity expense,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfExpenseEntity.insert(expense);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ExpenseEntity>> getExpenses(final String groupId) {
    final String _sql = "SELECT * FROM expenses WHERE groupId = ? AND isDeleted = 0 ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, groupId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"expenses"}, new Callable<List<ExpenseEntity>>() {
      @Override
      @NonNull
      public List<ExpenseEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfExpenseId = CursorUtil.getColumnIndexOrThrow(_cursor, "expenseId");
          final int _cursorIndexOfGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "groupId");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfAmountCents = CursorUtil.getColumnIndexOrThrow(_cursor, "amountCents");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final int _cursorIndexOfPaidBy = CursorUtil.getColumnIndexOrThrow(_cursor, "paidBy");
          final int _cursorIndexOfSplitAmong = CursorUtil.getColumnIndexOrThrow(_cursor, "splitAmong");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfHlcTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "hlcTimestamp");
          final List<ExpenseEntity> _result = new ArrayList<ExpenseEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ExpenseEntity _item;
            final String _tmpExpenseId;
            _tmpExpenseId = _cursor.getString(_cursorIndexOfExpenseId);
            final String _tmpGroupId;
            _tmpGroupId = _cursor.getString(_cursorIndexOfGroupId);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final long _tmpAmountCents;
            _tmpAmountCents = _cursor.getLong(_cursorIndexOfAmountCents);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            final String _tmpPaidBy;
            _tmpPaidBy = _cursor.getString(_cursorIndexOfPaidBy);
            final String _tmpSplitAmong;
            _tmpSplitAmong = _cursor.getString(_cursorIndexOfSplitAmong);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final boolean _tmpIsDeleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp != 0;
            final long _tmpHlcTimestamp;
            _tmpHlcTimestamp = _cursor.getLong(_cursorIndexOfHlcTimestamp);
            _item = new ExpenseEntity(_tmpExpenseId,_tmpGroupId,_tmpDescription,_tmpAmountCents,_tmpCurrency,_tmpPaidBy,_tmpSplitAmong,_tmpCreatedAt,_tmpIsDeleted,_tmpHlcTimestamp);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getExpensesList(final String groupId,
      final Continuation<? super List<ExpenseEntity>> $completion) {
    final String _sql = "SELECT * FROM expenses WHERE groupId = ? AND isDeleted = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, groupId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ExpenseEntity>>() {
      @Override
      @NonNull
      public List<ExpenseEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfExpenseId = CursorUtil.getColumnIndexOrThrow(_cursor, "expenseId");
          final int _cursorIndexOfGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "groupId");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfAmountCents = CursorUtil.getColumnIndexOrThrow(_cursor, "amountCents");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final int _cursorIndexOfPaidBy = CursorUtil.getColumnIndexOrThrow(_cursor, "paidBy");
          final int _cursorIndexOfSplitAmong = CursorUtil.getColumnIndexOrThrow(_cursor, "splitAmong");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfHlcTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "hlcTimestamp");
          final List<ExpenseEntity> _result = new ArrayList<ExpenseEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ExpenseEntity _item;
            final String _tmpExpenseId;
            _tmpExpenseId = _cursor.getString(_cursorIndexOfExpenseId);
            final String _tmpGroupId;
            _tmpGroupId = _cursor.getString(_cursorIndexOfGroupId);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final long _tmpAmountCents;
            _tmpAmountCents = _cursor.getLong(_cursorIndexOfAmountCents);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            final String _tmpPaidBy;
            _tmpPaidBy = _cursor.getString(_cursorIndexOfPaidBy);
            final String _tmpSplitAmong;
            _tmpSplitAmong = _cursor.getString(_cursorIndexOfSplitAmong);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final boolean _tmpIsDeleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp != 0;
            final long _tmpHlcTimestamp;
            _tmpHlcTimestamp = _cursor.getLong(_cursorIndexOfHlcTimestamp);
            _item = new ExpenseEntity(_tmpExpenseId,_tmpGroupId,_tmpDescription,_tmpAmountCents,_tmpCurrency,_tmpPaidBy,_tmpSplitAmong,_tmpCreatedAt,_tmpIsDeleted,_tmpHlcTimestamp);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getExpense(final String expenseId,
      final Continuation<? super ExpenseEntity> $completion) {
    final String _sql = "SELECT * FROM expenses WHERE expenseId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, expenseId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ExpenseEntity>() {
      @Override
      @Nullable
      public ExpenseEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfExpenseId = CursorUtil.getColumnIndexOrThrow(_cursor, "expenseId");
          final int _cursorIndexOfGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "groupId");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfAmountCents = CursorUtil.getColumnIndexOrThrow(_cursor, "amountCents");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final int _cursorIndexOfPaidBy = CursorUtil.getColumnIndexOrThrow(_cursor, "paidBy");
          final int _cursorIndexOfSplitAmong = CursorUtil.getColumnIndexOrThrow(_cursor, "splitAmong");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfHlcTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "hlcTimestamp");
          final ExpenseEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpExpenseId;
            _tmpExpenseId = _cursor.getString(_cursorIndexOfExpenseId);
            final String _tmpGroupId;
            _tmpGroupId = _cursor.getString(_cursorIndexOfGroupId);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final long _tmpAmountCents;
            _tmpAmountCents = _cursor.getLong(_cursorIndexOfAmountCents);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            final String _tmpPaidBy;
            _tmpPaidBy = _cursor.getString(_cursorIndexOfPaidBy);
            final String _tmpSplitAmong;
            _tmpSplitAmong = _cursor.getString(_cursorIndexOfSplitAmong);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final boolean _tmpIsDeleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp != 0;
            final long _tmpHlcTimestamp;
            _tmpHlcTimestamp = _cursor.getLong(_cursorIndexOfHlcTimestamp);
            _result = new ExpenseEntity(_tmpExpenseId,_tmpGroupId,_tmpDescription,_tmpAmountCents,_tmpCurrency,_tmpPaidBy,_tmpSplitAmong,_tmpCreatedAt,_tmpIsDeleted,_tmpHlcTimestamp);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
