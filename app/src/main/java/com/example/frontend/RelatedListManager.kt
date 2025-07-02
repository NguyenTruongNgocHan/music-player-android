import android.content.Context
import android.util.Log
import com.example.frontend.Track
import com.example.frontend.YouTubeService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RelatedListManager(private val context: Context, private val youTubeService: YouTubeService) {
    suspend fun loadRelatedList(currentTrack: Track): List<Track> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("RELATED_DEBUG", "Bắt đầu tìm kiếm bài liên quan cho: ${currentTrack.artist}")

                // 1. Log query tìm kiếm
                val searchQuery = currentTrack.artist
                Log.d("RELATED_DEBUG", "Search query: $searchQuery")

                // 2. Gọi API và log kết quả thô
                val rawResults = youTubeService.searchSongs(searchQuery)
                Log.d("RELATED_DEBUG", "Số lượng kết quả thô: ${rawResults.size}")
                rawResults.take(3).forEach {
                    Log.d("RELATED_DEBUG", "Mẫu kết quả: ${it.title} (ID: ${it.id})")
                }

                // 3. Log sau mỗi bước xử lý
                val afterFilter = rawResults.filter {
                    val keep = it.id != currentTrack.id
                    if (!keep) Log.d("RELATED_FILTER", "Loại bỏ trùng ID: ${it.title}")
                    keep
                }
                Log.d("RELATED_DEBUG", "Sau lọc ID hiện tại: ${afterFilter.size}")

                val afterDistinct = afterFilter.distinctBy { it.id }
                Log.d("RELATED_DEBUG", "Sau loại bỏ trùng lặp: ${afterDistinct.size}")

                val finalList = afterDistinct.shuffled().take(20)
                Log.d("RELATED_DEBUG", "Danh sách cuối cùng: ${finalList.size} bài")
                finalList.forEachIndexed { i, track ->
                    Log.d("RELATED_RESULT", "$i. ${track.title} - ${track.artist}")
                }

                finalList
            } catch (e: Exception) {
                Log.e("RELATED_ERROR", "Lỗi khi tải bài liên quan", e)
                emptyList()
            }
        }
    }
}
