/*
 *  This file is part of VidSnap.
 *
 *  VidSnap is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *  VidSnap is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  You should have received a copy of the GNU General Public License
 *  along with VidSnap.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.mugames.vidsnap.utility

/**
 * @author Udhaya
 * Created on 12-01-2023
 */


data class InstagramReelsTrayResponseModel(
    var tray: ArrayList<Tray> = arrayListOf(),
    var story_ranking_token: String? = null,
    var story_likes_config: StoryLikesConfig? = null,
    var rollcall_config: RollcallConfig? = null,
    var broadcasts: ArrayList<Any>? = null,
    var sticker_version: Int = 0,
    var face_filter_nux_version: Int = 0,
    var stories_viewer_gestures_nux_eligible: Boolean = false,
    var has_new_nux_story: Boolean = false,
    var refresh_window_ms: Int = 0,
    var response_timestamp: Int = 0,
    var status: String? = null
)

data class BloksSticker(
    var id: String? = null,
    var app_id: String? = null,
    var sticker_data: StickerData? = null,
    var bloks_sticker_type: String? = null,
)

data class Candidate(
    var width: Int = 0,
    var height: Int = 0,
    var url: String? = null, var scans_profile: String? = null
)

data class CommentInformTreatment(
    var should_have_inform_treatment: Boolean = false,
    var text: String? = null,
    var url: Any? = null,
    var action_type: Any? = null
)

data class FriendshipStatus(
    var muting: Boolean = false,
    var is_muting_reel: Boolean = false,
    var following: Boolean = false,
    var is_bestie: Boolean = false,
    var outgoing_request: Boolean = false,
)

data class IgArtist(
    var pk: Long = 0,
    var username: String? = null,
    var is_verified: Boolean = false,
    var profile_pic_id: String? = null,
    var profile_pic_url: String? = null,
    var fbid_v2: String? = null,
    var is_private: Boolean = false,
    var full_name: String? = null,
    var pk_id: String? = null
)

data class IgMention(
    var account_id: String? =
        null,
    var username: String? = null,
    var full_name: String? = null,
    var profile_pic_url: String? = null
)

data class ImageVersions2(
    var candidates: ArrayList<Candidate>? = null
)

data class Item(
    var taken_at: Int = 0,
    var pk: Long? = null,
    var id: String? = null,
    var device_timestamp: Any? = null,
    var media_type: Int = 0,
    var code: String? = null,
    var client_cache_key: String? = null,
    var filter_type: Int = 0,
    var is_unified_video: Boolean = false,
    var should_request_ads: Boolean = false,
    var original_media_has_visual_reply_media: Boolean = false,
    var caption_is_edited: Boolean = false,
    var like_and_view_counts_disabled: Boolean = false,
    var commerciality_status: String? = null,
    var is_paid_partnership: Boolean = false,
    var is_visual_reply_commenter_notice_enabled: Boolean = false,
    var clips_tab_pinned_user_ids: ArrayList<Any>? = null,
    var has_delayed_metadata: Boolean = false,
    var caption_position: Double = 0.0,
    var is_reel_media: Boolean = false,
    var photo_of_you: Boolean = false,
    var is_organic_product_tagging_eligible: Boolean = false,
    var can_see_insights_as_brand: Boolean = false,
    var image_versions2: ImageVersions2? = null,
    var original_width: Int = 0,
    var original_height: Int = 0,
    var caption: Any? =
        null,
    var comment_inform_treatment: CommentInformTreatment? = null,
    var sharing_friction_info: SharingFrictionInfo? = null,
    var can_viewer_save: Boolean = false,
    var is_in_profile_grid: Boolean = false,
    var profile_grid_control_enabled: Boolean = false,
    var organic_tracking_token: String? = null,
    var expiring_at: Int = 0,
    var imported_taken_at: Int = 0,
    var has_shared_to_fb: Int = 0,
    var product_type: String? = null,
    var deleted_reason: Int = 0,
    var integrity_review_decision: String? = null,
    var commerce_integrity_review_decision: Any? = null,
    var music_metadata: Any? = null,
    var is_artist_pick: Boolean = false,
    var user: User? = null,
    var can_reshare: Boolean = false,
    var can_reply: Boolean = false,
    var can_send_prompt: Boolean = false,
    var is_first_take: Boolean = false,
    var is_rollcall_v2: Boolean = false,
    var created_from_add_yours_browsing: Boolean = false,
    var story_feed_media: ArrayList<StoryFeedMedium>? = null,
    var story_static_models: ArrayList<Any>? = null,
    var supports_reel_reactions: Boolean = false,
    var can_send_custom_emojis: Boolean = false,
    var show_one_tap_fb_share_tooltip: Boolean = false,
    var location: Location? = null,
    var lat: Double = 0.0,
    var lng: Double = 0.0,
    var is_dash_eligible: Int = 0,
    var video_dash_manifest: String? =
        null,
    var video_codec: String? = null,
    var number_of_qualities: Int = 0,
    var video_versions: ArrayList<VideoVersion>? = null,
    var has_audio: Boolean = false,
    var video_duration: Double = 0.0,
    var story_locations: ArrayList<StoryLocation>? =
        null,
    var story_music_stickers: ArrayList<StoryMusicSticker>? = null,
    var story_bloks_stickers: ArrayList<StoryBloksSticker>? = null
)

data class Location(
    var pk: Long = 0,
    var short_name: String? = null,
    var facebook_places_id: Long = 0,
    var external_source: String? =
        null,
    var name: String? = null,
    var address: String? = null,
    var city: String? = null,
    var has_viewer_saved: Boolean = false,
    var lng: Double = 0.0,
    var lat: Double = 0.0,
    var is_eligible_for_guides: Boolean = false,
)

data class MusicAssetInfo(
    var audio_cluster_id: String? =
        null,
    var id: String? = null,
    var title: String? = null,
    var sanitized_title: Any? = null,
    var subtitle: String? = null,
    var display_artist: String? = null,
    var artist_id: Any? = null,
    var cover_artwork_uri: String? = null,
    var cover_artwork_thumbnail_uri: String? = null,
    var progressive_download_url: String? = null,
    var reactive_audio_download_url: Any? = null,
    var fast_start_progressive_download_url: String? = null,
    var web_30s_preview_download_url: Any? = null,
    var highlight_start_times_in_ms: ArrayList<Int>? = null,
    var is_explicit: Boolean = false,
    var dash_manifest: Any? = null,
    var has_lyrics: Boolean = false,
    var audio_asset_id: String? = null,
    var duration_in_ms: Int = 0,
    var dark_message: Any? = null,
    var allows_saving: Boolean = false,
    var territory_validity_periods: TerritoryValidityPeriods? = null,
    var ig_username: String? = null,
    var ig_artist: IgArtist? = null,
    var placeholder_profile_pic_url: String? = null,
    var should_mute_audio: Boolean = false,
    var should_mute_audio_reason: String? =
        null,
    var should_mute_audio_reason_type: Any? = null,
    var is_bookmarked: Boolean = false,
    var overlap_duration_in_ms: Int = 0,
    var audio_asset_start_time_in_ms: Int = 0,
    var allow_media_creation_with_music: Boolean = false,
    var is_trending_in_clips: Boolean = false,
    var formatted_clips_media_count: Any? =
        null,
    var streaming_services: Any? = null,
    var display_labels: Any? = null,
    var should_allow_music_editing: Boolean = false,
)

data class RollcallConfig(
    var is_unlocked: Boolean = false,
)

data class SharingFrictionInfo(
    var should_have_sharing_friction: Boolean = false,
    var bloks_app_url: Any? = null,
    var sharing_friction_payload: Any? = null
)

data class StickerData(
    var ig_mention: IgMention? = null
)

data class StoryBloksSticker(
    var bloks_sticker: BloksSticker? = null,
    var x: Double = 0.0,
    var y: Double = 0.0,
    var z: Int = 0,
    var width: Double = 0.0,
    var height: Double = 0.0,
    var rotation: Double = 0.0,
)

data class StoryFeedMedium(
    var x: Double = 0.0,
    var y: Double = 0.0,
    var z: Int = 0,
    var width: Double = 0.0,
    var height: Double = 0.0,
    var rotation: Double = 0.0,
    var is_pinned: Int = 0,
    var is_hidden: Int = 0,
    var is_sticker: Int = 0,
    var is_fb_sticker: Int = 0,
    var media_id: String? = null, var product_type: String? = null, var media_code: String? = null
)

data class StoryLikesConfig(
    var is_enabled: Boolean = false,
    var ufi_type: Int = 0,
)

data class StoryLocation(
    var x: Double = 0.0,
    var y: Double = 0.0,
    var z: Int = 0,
    var width: Double = 0.0,
    var height: Double = 0.0,
    var rotation: Double = 0.0,
    var is_pinned: Int = 0,
    var is_hidden: Int = 0,
    var is_sticker: Int = 0,
    var is_fb_sticker: Int = 0,
    var location: Location? = null
)

data class StoryMusicSticker(
    var x: Double = 0.0,
    var y: Double = 0.0,
    var z: Int = 0,
    var width: Double = 0.0, var height: Double = 0.0,
    var rotation: Double = 0.0,
    var is_pinned: Int = 0,
    var is_hidden: Int = 0,
    var is_sticker: Int = 0,
    var is_fb_sticker: Int = 0,
    var music_asset_info: MusicAssetInfo? = null
)

class TerritoryValidityPeriods()

data class Tray(
    var id: Any? = null,
    var latest_reel_media: Int = 0,
    var expiring_at: Int = 0,
    var seen: Int = 0,
    var can_reply: Boolean = false,
    var can_gif_quick_reply: Boolean = false,
    var can_reshare: Boolean = false,
    var can_react_with_avatar: Boolean = false,
    var reel_type: String? = null,
    var ad_expiry_timestamp_in_millis: Any? = null,
    var is_cta_sticker_available: Any? = null,
    var user: User? = null,
    var ranked_position: Int = 0, var seen_ranked_position: Int = 0,
    var muted: Boolean = false,
    var prefetch_count: Int = 0,
    var story_wedge_size: Int = 0,
    var has_besties_media: Boolean = false,
    var latest_besties_reel_media: Double = 0.0,
    var media_count: Int = 0,
    var media_ids: ArrayList<Any>? = null,
    var has_video: Boolean = false,
    var has_fan_club_media: Boolean = false,
    var items: ArrayList<Item>? = null,
    val owner: Owner? = null,
    var disabled_reply_types: ArrayList<String>? = null
)

public data class User(
    var pk: Any? = null,
    var username: String? = null,
    var is_verified: Boolean = false, var profile_pic_id: String? = null,
    var profile_pic_url: String? = null,
    var fbid_v2: String? = null,
    var is_private: Boolean = false, var full_name: String? =
        null,
    var pk_id: String? = null,
    var friendship_status: FriendshipStatus? = null
)

data class VideoVersion(
    var type: Int = 0,
    var width: Int = 0,
    var height: Int = 0,
    var url: String? = null,
    var id: String? = null
)

data class Owner(
    var type: String? = null,
    var pk: String? = null,
    var name: String? = null,
    var profile_pic_url: String? = null,
    var lat: Any? = null,
    var lng: Any? = null,
    var location_dict: Any? = null,
    var short_name: Any? = null,
    var profile_pic_username: String? = null,
    var challenge_id: Any? = null
)

