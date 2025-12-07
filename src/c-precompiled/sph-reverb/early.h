typedef struct {
  sp_time_t delay;
  sp_reverb_vec3_t direction_receiver;
  sp_reverb_vec3_t direction_source;
  uint32_t* triangle_index_chain;
  uint32_t triangle_index_count;
  uint32_t order;
  uint32_t path_type;
} sp_reverb_early_path_t;
typedef struct {
  sp_reverb_scene_t* scene;
  void* embree_device_handle;
  void* embree_scene_handle;
} sp_reverb_early_context_t;
typedef struct {
  sp_sample_t min_energy;
  sp_time_t max_delay;
  sp_sample_t max_path_length;
} sp_reverb_early_cutoff_t;
typedef struct {
  sp_reverb_vec3_t position_world;
  sp_reverb_vec3_t orientation_world;
} sp_reverb_early_source_t;
typedef struct {
  sp_reverb_vec3_t position_world;
  sp_reverb_vec3_t orientation_world;
} sp_reverb_early_receiver_t;
typedef struct {
  sp_reverb_early_path_t* path_list;
  uint32_t path_count;
} sp_reverb_early_path_set_t;
typedef struct {
  sp_channel_count_t channel_index;
  sp_sample_t gain;
  sp_time_t period;
  sp_time_t phase;
  sp_time_t duration;
} sp_reverb_early_partial_t;
typedef struct {
  sp_channel_count_t channel_index;
  sp_sample_t gain;
  sp_time_t band_period_start;
  sp_time_t band_period_end;
  sp_time_t duration;
} sp_reverb_early_noise_partial_t;
typedef struct {
  sp_sample_t* energy_envelope_per_band;
  uint32_t energy_envelope_sample_count;
  sp_sample_t* directional_histogram;
  uint32_t directional_bin_count;
  sp_sample_t* arrival_density;
  uint32_t arrival_density_sample_count;
} sp_reverb_early_statistics_t;
sp_reverb_early_path_set_t sp_reverb_early_paths_union(sp_reverb_early_path_set_t* path_set_list, uint32_t path_set_count);
sp_reverb_early_path_set_t sp_reverb_early_paths_cull(sp_reverb_scene_t* scene, sp_reverb_early_path_set_t* path_set, sp_reverb_early_cutoff_t* cutoff, sp_time_t max_path_count);
void sp_reverb_early_statistics_from_paths(sp_reverb_scene_t* scene, sp_reverb_early_path_set_t* path_set, sp_reverb_early_statistics_t* out_statistics);
sp_reverb_early_path_set_t sp_reverb_early_paths_diffraction(sp_reverb_early_context_t* context, uint32_t* edge_index_list, uint32_t edge_index_count, sp_reverb_early_cutoff_t* cutoff);
void sp_reverb_early_noise_partials_from_paths(sp_reverb_scene_t* scene, sp_reverb_early_path_set_t* path_set, sp_reverb_layout_t* layout, sp_time_t band_period_start, sp_time_t band_period_end, sp_time_t duration, sp_reverb_early_noise_partial_t* out_partial_list, sp_time_t out_partial_capacity, sp_time_t* out_partial_count);
void sp_reverb_early_partials_from_paths(sp_reverb_scene_t* scene, sp_reverb_early_path_set_t* path_set, sp_reverb_layout_t* layout, sp_reverb_sampled_partial_t* partial_list, sp_time_t partial_count, sp_reverb_early_partial_t* out_partial_list, sp_time_t out_partial_capacity, sp_time_t* out_partial_count);
void sp_reverb_early_context_init(sp_reverb_scene_t* scene, sp_reverb_early_context_t* out_context);
void sp_reverb_early_context_shutdown(sp_reverb_early_context_t* context);
sp_time_t sp_reverb_early_paths_image(sp_reverb_early_context_t* context, sp_reverb_early_source_t* source, sp_reverb_early_receiver_t* receiver, sp_reverb_early_cutoff_t* cutoff, sp_time_t path_capacity, sp_reverb_early_path_t* out_path_list);
void sp_reverb_early(sp_reverb_early_context_t* context, sp_reverb_early_source_t* source, sp_reverb_early_receiver_t* receiver, sp_reverb_layout_t* layout, sp_reverb_early_cutoff_t* cutoff, sp_reverb_sampled_partial_t* partial_list, sp_time_t partial_count, sp_reverb_early_partial_t* out_partial_list, sp_time_t out_partial_capacity, sp_time_t* out_partial_count);