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
import com.akeshari.splitblind.data.database.entity.GroupEntity;
import com.akeshari.splitblind.data.database.entity.MemberEntity;
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
public final class GroupDao_Impl implements GroupDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<GroupEntity> __insertionAdapterOfGroupEntity;

  private final EntityInsertionAdapter<MemberEntity> __insertionAdapterOfMemberEntity;

  public GroupDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfGroupEntity = new EntityInsertionAdapter<GroupEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `groups` (`groupId`,`name`,`createdBy`,`createdAt`,`groupKeyBase64`,`hlcTimestamp`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GroupEntity entity) {
        statement.bindString(1, entity.getGroupId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getCreatedBy());
        statement.bindLong(4, entity.getCreatedAt());
        statement.bindString(5, entity.getGroupKeyBase64());
        statement.bindLong(6, entity.getHlcTimestamp());
      }
    };
    this.__insertionAdapterOfMemberEntity = new EntityInsertionAdapter<MemberEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `members` (`groupId`,`memberId`,`displayName`,`joinedAt`,`isDeleted`,`hlcTimestamp`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MemberEntity entity) {
        statement.bindString(1, entity.getGroupId());
        statement.bindString(2, entity.getMemberId());
        statement.bindString(3, entity.getDisplayName());
        statement.bindLong(4, entity.getJoinedAt());
        final int _tmp = entity.isDeleted() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindLong(6, entity.getHlcTimestamp());
      }
    };
  }

  @Override
  public Object insertGroup(final GroupEntity group, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfGroupEntity.insert(group);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertMember(final MemberEntity member,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMemberEntity.insert(member);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<GroupEntity>> getAllGroups() {
    final String _sql = "SELECT * FROM groups ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"groups"}, new Callable<List<GroupEntity>>() {
      @Override
      @NonNull
      public List<GroupEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "groupId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCreatedBy = CursorUtil.getColumnIndexOrThrow(_cursor, "createdBy");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfGroupKeyBase64 = CursorUtil.getColumnIndexOrThrow(_cursor, "groupKeyBase64");
          final int _cursorIndexOfHlcTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "hlcTimestamp");
          final List<GroupEntity> _result = new ArrayList<GroupEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GroupEntity _item;
            final String _tmpGroupId;
            _tmpGroupId = _cursor.getString(_cursorIndexOfGroupId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpCreatedBy;
            _tmpCreatedBy = _cursor.getString(_cursorIndexOfCreatedBy);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpGroupKeyBase64;
            _tmpGroupKeyBase64 = _cursor.getString(_cursorIndexOfGroupKeyBase64);
            final long _tmpHlcTimestamp;
            _tmpHlcTimestamp = _cursor.getLong(_cursorIndexOfHlcTimestamp);
            _item = new GroupEntity(_tmpGroupId,_tmpName,_tmpCreatedBy,_tmpCreatedAt,_tmpGroupKeyBase64,_tmpHlcTimestamp);
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
  public Object getGroup(final String groupId,
      final Continuation<? super GroupEntity> $completion) {
    final String _sql = "SELECT * FROM groups WHERE groupId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, groupId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<GroupEntity>() {
      @Override
      @Nullable
      public GroupEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "groupId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCreatedBy = CursorUtil.getColumnIndexOrThrow(_cursor, "createdBy");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfGroupKeyBase64 = CursorUtil.getColumnIndexOrThrow(_cursor, "groupKeyBase64");
          final int _cursorIndexOfHlcTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "hlcTimestamp");
          final GroupEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpGroupId;
            _tmpGroupId = _cursor.getString(_cursorIndexOfGroupId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpCreatedBy;
            _tmpCreatedBy = _cursor.getString(_cursorIndexOfCreatedBy);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpGroupKeyBase64;
            _tmpGroupKeyBase64 = _cursor.getString(_cursorIndexOfGroupKeyBase64);
            final long _tmpHlcTimestamp;
            _tmpHlcTimestamp = _cursor.getLong(_cursorIndexOfHlcTimestamp);
            _result = new GroupEntity(_tmpGroupId,_tmpName,_tmpCreatedBy,_tmpCreatedAt,_tmpGroupKeyBase64,_tmpHlcTimestamp);
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

  @Override
  public Flow<List<MemberEntity>> getMembers(final String groupId) {
    final String _sql = "SELECT * FROM members WHERE groupId = ? AND isDeleted = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, groupId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"members"}, new Callable<List<MemberEntity>>() {
      @Override
      @NonNull
      public List<MemberEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "groupId");
          final int _cursorIndexOfMemberId = CursorUtil.getColumnIndexOrThrow(_cursor, "memberId");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfJoinedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "joinedAt");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfHlcTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "hlcTimestamp");
          final List<MemberEntity> _result = new ArrayList<MemberEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MemberEntity _item;
            final String _tmpGroupId;
            _tmpGroupId = _cursor.getString(_cursorIndexOfGroupId);
            final String _tmpMemberId;
            _tmpMemberId = _cursor.getString(_cursorIndexOfMemberId);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final long _tmpJoinedAt;
            _tmpJoinedAt = _cursor.getLong(_cursorIndexOfJoinedAt);
            final boolean _tmpIsDeleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp != 0;
            final long _tmpHlcTimestamp;
            _tmpHlcTimestamp = _cursor.getLong(_cursorIndexOfHlcTimestamp);
            _item = new MemberEntity(_tmpGroupId,_tmpMemberId,_tmpDisplayName,_tmpJoinedAt,_tmpIsDeleted,_tmpHlcTimestamp);
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
  public Object getMembersList(final String groupId,
      final Continuation<? super List<MemberEntity>> $completion) {
    final String _sql = "SELECT * FROM members WHERE groupId = ? AND isDeleted = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, groupId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MemberEntity>>() {
      @Override
      @NonNull
      public List<MemberEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "groupId");
          final int _cursorIndexOfMemberId = CursorUtil.getColumnIndexOrThrow(_cursor, "memberId");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfJoinedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "joinedAt");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfHlcTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "hlcTimestamp");
          final List<MemberEntity> _result = new ArrayList<MemberEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MemberEntity _item;
            final String _tmpGroupId;
            _tmpGroupId = _cursor.getString(_cursorIndexOfGroupId);
            final String _tmpMemberId;
            _tmpMemberId = _cursor.getString(_cursorIndexOfMemberId);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final long _tmpJoinedAt;
            _tmpJoinedAt = _cursor.getLong(_cursorIndexOfJoinedAt);
            final boolean _tmpIsDeleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp != 0;
            final long _tmpHlcTimestamp;
            _tmpHlcTimestamp = _cursor.getLong(_cursorIndexOfHlcTimestamp);
            _item = new MemberEntity(_tmpGroupId,_tmpMemberId,_tmpDisplayName,_tmpJoinedAt,_tmpIsDeleted,_tmpHlcTimestamp);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
