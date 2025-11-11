
#ifndef sph_reverb_h_included
#define sph_reverb_h_included

#include <inttypes.h>
#include <stddef.h>

#ifndef sp_sample_t
#define sp_sample_t double
#endif
#ifndef sp_time_t
#define sp_time_t uint32_t
#endif
#ifndef sp_channel_count_t
#define sp_channel_count_t uint8_t
#endif
typedef struct {
  sp_sample_t gain;
  sp_time_t decay;
  sp_time_t phase;
} sp_reverb_response_t;
typedef struct {
  sp_sample_t* bases;
  sp_channel_count_t channel_count;
  sp_time_t basis_length;
} sp_reverb_layout_t;
typedef struct {
  sp_time_t* delays;
  sp_time_t delay_count;
  sp_sample_t* mix_row_major;
  sp_time_t mix_rows;
  sp_time_t mix_columns;
  sp_time_t* band_frequencies;
  sp_sample_t* band_gains;
  sp_time_t band_count;
  sp_sample_t strength;
} sp_reverb_late_config_t;
void sp_reverb_late_table(sp_reverb_late_config_t* config, sp_sample_t* frequencies, size_t frequency_count, sp_reverb_response_t* out_responses);
sp_reverb_response_t sp_reverb_late_lookup(sp_sample_t* frequency_list, sp_reverb_response_t* response_list, sp_time_t response_count, sp_sample_t query_frequency);
void sp_reverb_late_project(sp_reverb_response_t* response, sp_reverb_layout_t* layout, sp_sample_t* out_channel_gains, size_t out_channel_count);
#endif
