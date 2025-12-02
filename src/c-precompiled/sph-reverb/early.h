typedef struct {
  float x;
  float y;
  float z;
} sp_reverb_vec3_t;
typedef struct {
  sp_reverb_vec3_t* vertex_list;
  uint32_t vertex_count;
  uint32_t* index_list;
  uint32_t index_count;
  uint32_t* face_wall_index_list;
  uint32_t face_count;
  void* embree_scene_handle;
} sp_reverb_early_geometry_t;
typedef struct {
  sp_time_t* band_period_list;
  sp_time_t band_count;
  sp_sample_t* wall_reflectivity_list;
  uint32_t wall_count;
  sp_sample_t* air_attenuation_list;
} sp_reverb_early_materials_t;
typedef struct {
  sp_reverb_early_geometry_t geometry;
  sp_reverb_early_materials_t materials;
  void* embree_device_handle;
  void* embree_scene_handle;
} sp_reverb_early_scene_t;
typedef struct {
  sp_reverb_vec3_t position_world;
  sp_reverb_vec3_t orientation_world;
} sp_reverb_early_source_t;
typedef struct {
  sp_reverb_vec3_t position_world;
  sp_reverb_vec3_t orientation_world;
} sp_reverb_early_receiver_t;
typedef struct {
  sp_time_t delay;
  sp_sample_t gain;
  sp_reverb_vec3_t direction;
  uint32_t* wall_index_chain;
  uint32_t wall_index_count;
  uint32_t order;
  sp_sample_t* band_gain_list;
  sp_time_t band_count;
} sp_reverb_early_path_t;
typedef struct {
  sp_reverb_early_path_t* path_list;
  sp_time_t path_count;
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
void sp_reverb_early_build_scene(sp_reverb_early_geometry_t* geometry, sp_reverb_early_materials_t* materials, sp_reverb_early_scene_t* out_scene);
sp_time_t sp_reverb_early_paths_image(sp_reverb_early_scene_t* scene, sp_reverb_early_source_t* source, sp_reverb_early_receiver_t* receiver, sp_time_t max_order, sp_time_t path_capacity, sp_reverb_early_path_t* out_path_list);
sp_reverb_early_path_set_t sp_reverb_early_paths_beam(sp_reverb_early_scene_t* scene, sp_reverb_early_source_t* source, sp_reverb_early_receiver_t* receiver, sp_time_t max_order, sp_time_t path_cap, uint32_t* portal_index_list, uint32_t portal_index_count);
sp_reverb_early_path_set_t sp_reverb_early_paths_diffraction(sp_reverb_early_scene_t* scene, uint32_t* edge_index_list, uint32_t edge_index_count, sp_time_t* band_period_list, sp_time_t band_count);
sp_reverb_early_path_set_t sp_reverb_early_paths_union(sp_reverb_early_path_set_t* path_set_list, uint32_t path_set_count);
sp_reverb_early_path_set_t sp_reverb_early_paths_cull(sp_reverb_early_path_set_t* path_set, sp_sample_t threshold, sp_time_t max_paths);
void sp_reverb_early_noise_partials_from_paths(sp_reverb_early_path_set_t* path_set, sp_reverb_layout_t* layout, sp_time_t band_period_start, sp_time_t band_period_end, sp_time_t duration, sp_reverb_early_noise_partial_t* out_partial_list, sp_time_t out_partial_capacity, sp_time_t* out_partial_count);
void sp_reverb_early_partials_from_paths(sp_reverb_early_path_set_t* path_set, sp_reverb_layout_t* layout, sp_reverb_sampled_partial_t* partial_list, sp_time_t partial_count, sp_reverb_early_partial_t* out_partial_list, sp_time_t out_partial_capacity, sp_time_t* out_partial_count);
void sp_reverb_early(sp_reverb_early_scene_t* scene, sp_reverb_early_source_t* source, sp_reverb_early_receiver_t* receiver, sp_reverb_layout_t* layout, sp_reverb_sampled_partial_t* partial_list, sp_time_t partial_count, sp_reverb_early_partial_t* out_partial_list, sp_time_t out_partial_capacity, sp_time_t* out_partial_count);