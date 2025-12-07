
#ifndef sph_reverb_h_included
#define sph_reverb_h_included

#include <inttypes.h>

#ifndef sp_sample_t
#define sp_sample_t double
#endif
#ifndef sp_time_t
#define sp_time_t uint32_t
#endif
#ifndef sp_pi
#define sp_pi 3.141592653589793
#endif
#ifndef sp_channel_count_t
#define sp_channel_count_t uint8_t
#endif
#ifndef sp_reverb_position_max_dimensions
#define sp_reverb_position_max_dimensions 8
#endif
#ifndef sp_rate
#define sp_rate ((sp_sample_t)(48000))
#endif
#ifndef sp_reverb_sound_meter_sample
#define sp_reverb_sound_meter_sample (sp_rate / ((sp_sample_t)(343.0)))
#endif
#ifndef sp_bool_t
#define sp_bool_t uint8_t
#endif
typedef struct {
  sp_sample_t gain;
  sp_time_t period;
  sp_time_t phase;
} sp_reverb_sampled_partial_t;
typedef struct {
  sp_sample_t values[sp_reverb_position_max_dimensions];
  uint8_t dimension_count;
} sp_reverb_position_t;
typedef struct {
  sp_sample_t* bases;
  sp_reverb_position_t* channel_position_list;
  sp_channel_count_t channel_count;
  uint8_t basis_length;
} sp_reverb_layout_t;
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
  uint32_t* triangle_material_index_list;
  uint32_t triangle_count;
} sp_reverb_geometry_t;
typedef struct {
  sp_time_t* band_period_list;
  uint32_t band_count;
  sp_sample_t* material_reflectivity_list;
  uint32_t material_count;
  sp_sample_t* air_attenuation_list;
} sp_reverb_materials_t;
typedef struct {
  sp_reverb_geometry_t geometry;
  sp_reverb_materials_t materials;
} sp_reverb_scene_t;
#include <sph-reverb/late.h>
#include <sph-reverb/early.h>
#endif
