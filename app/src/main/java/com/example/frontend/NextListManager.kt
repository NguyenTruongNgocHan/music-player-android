import android.content.Context
import android.util.Log
import com.example.frontend.Track
import com.example.frontend.YouTubeService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NextListManager(private val context: Context, private val youTubeService: YouTubeService) {

    suspend fun loadNextList(currentTrack: Track): List<Track> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("NEXT_DEBUG", "Bắt đầu tải danh sách Next cho bài: ${currentTrack.title} (ID: ${currentTrack.id})")

                // 1. Log query ngẫu nhiên
                val randomQuery = listOf(
                    "official mv",
                    "english mv",
                    "chinese mv",
                    "korea mv",
                    "japanese mv"
                ).random().also {
                    Log.d("NEXT_DEBUG", "Sử dụng query ngẫu nhiên: '$it'")
                }

                // 2. Log kết quả API thô
                val rawResults = youTubeService.searchSongs(randomQuery)
                Log.d("NEXT_DEBUG", "Nhận được ${rawResults.size} kết quả thô từ API")
                rawResults.take(3).forEachIndexed { index, track ->
                    Log.d("NEXT_DEBUG", "Kết quả thô ${index + 1}: ${track.title} | ID: ${track.id}")
                }

                // 3. Log sau khi lọc
                val afterFilter = rawResults.filter {
                    val shouldKeep = it.id != currentTrack.id
                    if (!shouldKeep) {
                        Log.d("NEXT_FILTER", "Đã lọc bỏ: ${it.title} (ID trùng với bài hiện tại)")
                    }
                    shouldKeep
                }
                Log.d("NEXT_DEBUG", "Còn lại ${afterFilter.size} bài sau lọc")

                // 4. Log sau khi loại bỏ trùng lặp
                val afterDistinct = afterFilter.distinctBy {
                    Log.d("NEXT_DISTINCT", "Kiểm tra ID: ${it.id}")
                    it.id
                }
                Log.d("NEXT_DEBUG", "Còn lại ${afterDistinct.size} bài sau distinctBy")

                // 5. Log kết quả cuối
                val finalList = afterDistinct.shuffled().take(20)
                Log.d("NEXT_RESULT", "Danh sách cuối cùng có ${finalList.size} bài:")
                finalList.forEachIndexed { index, track ->
                    Log.d("NEXT_RESULT", "${index + 1}. ${track.title} - ${track.artist} (ID: ${track.id})")
                }

                finalList
            } catch (e: Exception) {
                Log.e("NEXT_ERROR", "Lỗi khi tải Next list", e)
                emptyList<Track>().also {
                    Log.d("NEXT_DEBUG", "Trả về danh sách rỗng do lỗi")
                }
            }
        }
    }
}
