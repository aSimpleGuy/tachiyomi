package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.Queries
import com.pushtorefresh.storio.sqlite.operations.get.PreparedGetListOfObjects
import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.Query
import com.pushtorefresh.storio.sqlite.queries.RawQuery
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.SourceIdMangaCount
import eu.kanade.tachiyomi.data.database.resolvers.LibraryMangaGetResolver
import eu.kanade.tachiyomi.data.database.resolvers.MangaCoverLastModifiedPutResolver
import eu.kanade.tachiyomi.data.database.resolvers.MangaFavoritePutResolver
import eu.kanade.tachiyomi.data.database.resolvers.MangaFlagsPutResolver
import eu.kanade.tachiyomi.data.database.resolvers.MangaLastUpdatedPutResolver
import eu.kanade.tachiyomi.data.database.resolvers.MangaTitlePutResolver
import eu.kanade.tachiyomi.data.database.resolvers.SourceIdMangaCountGetResolver
import eu.kanade.tachiyomi.data.database.tables.CategoryTable
import eu.kanade.tachiyomi.data.database.tables.ChapterTable
import eu.kanade.tachiyomi.data.database.tables.MangaCategoryTable
import eu.kanade.tachiyomi.data.database.tables.MangaTable

interface MangaQueries : DbProvider {

    fun getLibraryMangas() = db.get()
        .listOfObjects(LibraryManga::class.java)
        .withQuery(
            RawQuery.builder()
                .query(libraryQuery)
                .observesTables(MangaTable.TABLE, ChapterTable.TABLE, MangaCategoryTable.TABLE, CategoryTable.TABLE)
                .build(),
        )
        .withGetResolver(LibraryMangaGetResolver.INSTANCE)
        .prepare()

    fun getDuplicateLibraryManga(manga: Manga) = db.get()
        .`object`(Manga::class.java)
        .withQuery(
            Query.builder()
                .table(MangaTable.TABLE)
                .where("${MangaTable.COL_FAVORITE} = 1 AND LOWER(${MangaTable.COL_TITLE}) = ? AND ${MangaTable.COL_SOURCE} != ?")
                .whereArgs(
                    manga.title.lowercase(),
                    manga.source,
                )
                .limit(1)
                .build(),
        )
        .prepare()

    fun getFavoriteMangas(sortByTitle: Boolean = true): PreparedGetListOfObjects<Manga> {
        var queryBuilder = Query.builder()
            .table(MangaTable.TABLE)
            .where("${MangaTable.COL_FAVORITE} = ?")
            .whereArgs(1)

        if (sortByTitle) {
            queryBuilder = queryBuilder.orderBy(MangaTable.COL_TITLE)
        }

        return db.get()
            .listOfObjects(Manga::class.java)
            .withQuery(queryBuilder.build())
            .prepare()
    }

    fun getManga(url: String, sourceId: Long) = db.get()
        .`object`(Manga::class.java)
        .withQuery(
            Query.builder()
                .table(MangaTable.TABLE)
                .where("${MangaTable.COL_URL} = ? AND ${MangaTable.COL_SOURCE} = ?")
                .whereArgs(url, sourceId)
                .build(),
        )
        .prepare()

    fun getManga(id: Long) = db.get()
        .`object`(Manga::class.java)
        .withQuery(
            Query.builder()
                .table(MangaTable.TABLE)
                .where("${MangaTable.COL_ID} = ?")
                .whereArgs(id)
                .build(),
        )
        .prepare()

    fun getSourceIdsWithNonLibraryManga() = db.get()
        .listOfObjects(SourceIdMangaCount::class.java)
        .withQuery(
            RawQuery.builder()
                .query(getSourceIdsWithNonLibraryMangaQuery())
                .observesTables(MangaTable.TABLE)
                .build(),
        )
        .withGetResolver(SourceIdMangaCountGetResolver.INSTANCE)
        .prepare()

    fun insertManga(manga: Manga) = db.put().`object`(manga).prepare()

    fun insertMangas(mangas: List<Manga>) = db.put().objects(mangas).prepare()

    fun updateChapterFlags(manga: Manga) = db.put()
        .`object`(manga)
        .withPutResolver(MangaFlagsPutResolver(MangaTable.COL_CHAPTER_FLAGS, Manga::chapter_flags))
        .prepare()

    fun updateChapterFlags(manga: List<Manga>) = db.put()
        .objects(manga)
        .withPutResolver(MangaFlagsPutResolver(MangaTable.COL_CHAPTER_FLAGS, Manga::chapter_flags))
        .prepare()

    fun updateViewerFlags(manga: Manga) = db.put()
        .`object`(manga)
        .withPutResolver(MangaFlagsPutResolver(MangaTable.COL_VIEWER, Manga::viewer_flags))
        .prepare()

    fun updateViewerFlags(manga: List<Manga>) = db.put()
        .objects(manga)
        .withPutResolver(MangaFlagsPutResolver(MangaTable.COL_VIEWER, Manga::viewer_flags))
        .prepare()

    fun updateLastUpdated(manga: Manga) = db.put()
        .`object`(manga)
        .withPutResolver(MangaLastUpdatedPutResolver())
        .prepare()

    fun updateMangaFavorite(manga: Manga) = db.put()
        .`object`(manga)
        .withPutResolver(MangaFavoritePutResolver())
        .prepare()

    fun updateMangaTitle(manga: Manga) = db.put()
        .`object`(manga)
        .withPutResolver(MangaTitlePutResolver())
        .prepare()

    fun updateMangaCoverLastModified(manga: Manga) = db.put()
        .`object`(manga)
        .withPutResolver(MangaCoverLastModifiedPutResolver())
        .prepare()

    fun deleteManga(manga: Manga) = db.delete().`object`(manga).prepare()

    fun deleteMangas(mangas: List<Manga>) = db.delete().objects(mangas).prepare()

    fun deleteMangasNotInLibraryBySourceIds(sourceIds: List<Long>) = db.delete()
        .byQuery(
            DeleteQuery.builder()
                .table(MangaTable.TABLE)
                .where("${MangaTable.COL_FAVORITE} = ? AND ${MangaTable.COL_SOURCE} IN (${Queries.placeholders(sourceIds.size)})")
                .whereArgs(0, *sourceIds.toTypedArray())
                .build(),
        )
        .prepare()

    fun deleteMangas() = db.delete()
        .byQuery(
            DeleteQuery.builder()
                .table(MangaTable.TABLE)
                .build(),
        )
        .prepare()

    fun getLastReadManga() = db.get()
        .listOfObjects(Manga::class.java)
        .withQuery(
            RawQuery.builder()
                .query(getLastReadMangaQuery())
                .observesTables(MangaTable.TABLE)
                .build(),
        )
        .prepare()

    fun getTotalChapterManga() = db.get()
        .listOfObjects(Manga::class.java)
        .withQuery(
            RawQuery.builder()
                .query(getTotalChapterMangaQuery())
                .observesTables(MangaTable.TABLE)
                .build(),
        )
        .prepare()

    fun getLatestChapterManga() = db.get()
        .listOfObjects(Manga::class.java)
        .withQuery(
            RawQuery.builder()
                .query(getLatestChapterMangaQuery())
                .observesTables(MangaTable.TABLE)
                .build(),
        )
        .prepare()

    fun getChapterFetchDateManga() = db.get()
        .listOfObjects(Manga::class.java)
        .withQuery(
            RawQuery.builder()
                .query(getChapterFetchDateMangaQuery())
                .observesTables(MangaTable.TABLE)
                .build(),
        )
        .prepare()
}
